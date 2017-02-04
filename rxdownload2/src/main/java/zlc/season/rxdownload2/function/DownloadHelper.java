package zlc.season.rxdownload2.function;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.FlowableEmitter;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import zlc.season.rxdownload2.entity.DownloadRange;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownload2.entity.DownloadType;
import zlc.season.rxdownload2.entity.DownloadTypeFactory;
import zlc.season.rxdownload2.entity.TemporaryRecord;
import zlc.season.rxdownload2.entity.TemporaryRecordTable;

import static zlc.season.rxdownload2.function.Constant.CONTEXT_NULL_HINT;
import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_RECORD_FILE_DAMAGED;
import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_URL_EXISTS;
import static zlc.season.rxdownload2.function.Constant.TEST_RANGE_SUPPORT;
import static zlc.season.rxdownload2.function.Utils.contentLength;
import static zlc.season.rxdownload2.function.Utils.empty;
import static zlc.season.rxdownload2.function.Utils.installApk;
import static zlc.season.rxdownload2.function.Utils.lastModify;
import static zlc.season.rxdownload2.function.Utils.log;
import static zlc.season.rxdownload2.function.Utils.notSupportRange;
import static zlc.season.rxdownload2.function.Utils.requestRangeNotSatisfiable;
import static zlc.season.rxdownload2.function.Utils.serverFileChanged;
import static zlc.season.rxdownload2.function.Utils.serverFileNotChange;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:39
 * Download helper 类
 */
public class DownloadHelper {

    private int MAX_RETRY_COUNT = 3;

    private DownloadApi mDownloadApi;
    private FileHelper mFileHelper;
    private DownloadTypeFactory type;

    //Record : { "url" : new String[] { "file path" , "temp file path" , "last modify file path" }}
    private Map<String, TemporaryRecord> temporaryRecord;
    private TemporaryRecordTable recordTable;

    public DownloadHelper() {
        temporaryRecord = new HashMap<>();
        mFileHelper = new FileHelper();
        mDownloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
        type = new DownloadTypeFactory(this);

        recordTable = new TemporaryRecordTable();
    }

    public void setRetrofit(Retrofit retrofit) {
        mDownloadApi = retrofit.create(DownloadApi.class);
    }

    public void setDefaultSavePath(String defaultSavePath) {
        mFileHelper.setDefaultSavePath(defaultSavePath);
    }

    public int getMaxRetryCount() {
        return MAX_RETRY_COUNT;
    }

    public void setMaxRetryCount(int MAX_RETRY_COUNT) {
        this.MAX_RETRY_COUNT = MAX_RETRY_COUNT;
    }

    public String[] getFileSavePaths(String savePath) {
        return mFileHelper.getRealDirectoryPaths(savePath);
    }

    public String[] getRealFilePaths(String saveName, String savePath) {
        return mFileHelper.getRealFilePaths(saveName, savePath);
    }

    public DownloadApi getDownloadApi() {
        return mDownloadApi;
    }

    public int getMaxThreads() {
        return mFileHelper.getMaxThreads();
    }

    public void setMaxThreads(int MAX_THREADS) {
        mFileHelper.setMaxThreads(MAX_THREADS);
    }

    public void prepareNormalDownload(String url, long fileLength, String lastModify)
            throws IOException, ParseException {

        mFileHelper.prepareDownload(getLastModifyFile(url),
                getFile(url), fileLength, lastModify);
    }

    public void saveNormalFile(
            FlowableEmitter<DownloadStatus> emitter,
            String url, Response<ResponseBody> resp) {

        mFileHelper.saveFile(emitter, getFile(url), resp);
    }

    public DownloadRange readDownloadRange(String url, int i)
            throws IOException {
        return mFileHelper.readDownloadRange(getTempFile(url), i);
    }

    public void prepareMultiThreadDownload(
            String url, long fileLength, String lastModify)
            throws IOException, ParseException {

        mFileHelper.prepareDownload(getLastModifyFile(url),
                getTempFile(url), getFile(url),
                fileLength, lastModify);
    }

    public void saveRangeFile(
            FlowableEmitter<DownloadStatus> emitter,
            int i, long start, long end,
            String url, ResponseBody response) {

        mFileHelper.saveFile(emitter, i, start, end,
                getTempFile(url), getFile(url), response);
    }

    /**
     * dispatch download
     *
     * @param url         url for download
     * @param saveName    save name
     * @param savePath    save path
     * @param context     context
     * @param autoInstall auto install
     * @return DownloadStatus
     */
    public Observable<DownloadStatus> downloadDispatcher(final String url,
                                                         final String saveName, final String savePath,
                                                         final Context context, final boolean autoInstall) {

        try {
            beforeDownload(url, saveName, savePath);
        } catch (IOException e) {
            return Observable.error(e);
        }

        return getDownloadType(url)
                .flatMap(new Function<DownloadType, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(DownloadType downloadType)
                            throws Exception {
                        downloadType.prepareDownload();
                        return downloadType.startDownload();
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        autoInstall(autoInstall, context, saveName, savePath);
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (throwable instanceof CompositeException) {
                            CompositeException realException = (CompositeException) throwable;
                            List<Throwable> exceptions = realException.getExceptions();
                            for (Throwable each : exceptions) {
                                log(each);
                            }
                        } else {
                            log(throwable);
                        }
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        recordTable.delete(url);
                    }
                });
    }

    /**
     * some server such IIS does not support HEAD method, use GET.
     *
     * @param url url
     * @return DownloadType
     * @throws IOException
     */
    public Observable<DownloadType> notSupportHead(final String url)
            throws IOException {

        return mDownloadApi
                .GET(TEST_RANGE_SUPPORT, readLastModify(url), url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> response)
                            throws Exception {
                        if (serverFileNotChange(response)) {
                            return getServerFileNotChangedType(response, url);
                        } else if (serverFileChanged(response)) {
                            return getServerFileChangedType(response, url);
                        } else {
                            return type.unable();
                        }
                    }
                })
                .compose(Utils.<DownloadType>retry(MAX_RETRY_COUNT));
    }

    private void autoInstall(boolean autoInstall, Context context,
                             String saveName, String savePath) {
        if (autoInstall) {
            if (context == null) {
                throw new IllegalArgumentException(CONTEXT_NULL_HINT);
            }
            installApk(context, new File(getRealFilePaths(saveName, savePath)[0]));
        }
    }

    private void beforeDownload(String url, String saveName, String savePath)
            throws IOException {

        if (recordTable.exists(url)) {
            throw new IOException(DOWNLOAD_URL_EXISTS);
        }

        addDownloadRecord(url, saveName, savePath);
    }

    private void addDownloadRecord(String url, String saveName, String savePath)
            throws IOException {

        mFileHelper.createDownloadDirs(savePath);
        recordTable.add(url, new TemporaryRecord());
//        temporaryRecord.put(url, getRealFilePaths(saveName, savePath));
    }

    private String readLastModify(String url)
            throws IOException {
        return mFileHelper.getLastModify(getLastModifyFile(url));
    }

    private boolean downloadNotComplete(String url)
            throws IOException {
        return mFileHelper.downloadNotComplete(getTempFile(url));
    }

    private boolean downloadNotComplete(String url, long contentLength) {
        return getFile(url).length() != contentLength;
    }

    private boolean needReDownload(String url, long contentLength)
            throws IOException {
        return tempFileNotExists(url) || tempFileDamaged(url, contentLength);
    }

    private boolean downloadFileExists(String url) {
        return getFile(url).exists();
    }

    private boolean tempFileDamaged(String url, long fileLength)
            throws IOException {
        return mFileHelper.tempFileDamaged(getTempFile(url), fileLength);
    }

    private boolean tempFileNotExists(String url) {
        return !getTempFile(url).exists();
    }

    private File getFile(String url) {
        return recordTable.getFile(url);
    }

    private File getTempFile(String url) {
        return recordTable.getTempFile(url);
    }

    private File getLastModifyFile(String url) {
        return recordTable.getLastModifyFile(url);
    }


    /**
     * 获取下载类型
     *
     * @param url url
     * @return download type
     */
    private Observable<DownloadType> getDownloadType(final String url) {
        return Observable.just(1)
                .flatMap(new Function<Integer, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Integer integer) throws Exception {
                        TemporaryRecord record = recordTable.get(url);
                        boolean isEmpty = empty(record.getSaveName());

                        if (isEmpty) {
                            return getEmptyNameType(url);
                        } else {
                            return getNotEmptyType(url);
                        }
                    }
                });
    }

    private ObservableSource<DownloadType> getNotEmptyType(String url) {
        if (downloadFileExists(url)) {
            try {
                return getFileExistsType(url);
            } catch (IOException e) {
                return getFileNotExistsType(url);
            }
        } else {
            return getFileNotExistsType(url);
        }
    }

    private Observable<DownloadType> getEmptyNameType(final String url) {
        return mDownloadApi.HEAD(TEST_RANGE_SUPPORT, url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(Response<Void> resp) throws Exception {
                        String fileName = Utils.contentDisposition(resp);
                        if (empty(fileName)) {
                            fileName = url.substring(url.lastIndexOf("/"));
                        }
                        recordTable.update(url, fileName);
                        recordTable.update(url, Utils.notSupportRange(resp));
                    }
                })
                .flatMap(new Function<Response<Void>, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Response<Void> resp) throws Exception {
                        if (downloadFileExists(url)) {
                            try {
                                return getFileExistsType(url);
                            } catch (IOException e) {
                                return getFileNotExistsType(url, resp);
                            }
                        } else {
                            return getFileNotExistsType(url, resp);
                        }
                    }
                });
    }

    private Observable<DownloadType> getFileNotExistsType(final String url,
                                                          final Response<Void> response) {
        return Observable.just(1)
                .map(new Function<Integer, DownloadType>() {
                    @Override
                    public DownloadType apply(Integer integer) throws Exception {
                        if (notSupportRange(response)) {
                            return type.normal(url, contentLength(response), lastModify(response));
                        } else {
                            return type.multithread(url, contentLength(response), lastModify(response));
                        }
                    }
                });
    }

    private Observable<DownloadType> getFileNotExistsType(final String url) {
        return mDownloadApi.HEAD(TEST_RANGE_SUPPORT, url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> response)
                            throws Exception {
                        if (notSupportRange(response)) {
                            return type.normal(url, contentLength(response), lastModify(response));
                        } else {
                            return type.multithread(url, contentLength(response), lastModify(response));
                        }
                    }
                })
                .compose(Utils.<DownloadType>retry(MAX_RETRY_COUNT));
    }

    private Observable<DownloadType> getFileExistsType(final String url)
            throws IOException {
        return mDownloadApi.HEAD(TEST_RANGE_SUPPORT, readLastModify(url), url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> resp)
                            throws Exception {
                        if (serverFileNotChange(resp)) {
                            return getServerFileNotChangedType(resp, url);
                        } else if (serverFileChanged(resp)) {
                            return getServerFileChangedType(resp, url);
                        } else if (requestRangeNotSatisfiable(resp)) {
                            return type.needGET(url, contentLength(resp), lastModify(resp));
                        } else {
                            return type.unable();
                        }
                    }
                })
                .compose(Utils.<DownloadType>retry(MAX_RETRY_COUNT));
    }

    private DownloadType getServerFileChangedType(Response<Void> resp, String url) {
        if (notSupportRange(resp)) {
            return type.normal(url,
                    contentLength(resp), lastModify(resp));
        } else {
            return type.multithread(url,
                    contentLength(resp), lastModify(resp));
        }
    }

    private DownloadType getServerFileNotChangedType(Response<Void> resp, String url) {
        if (notSupportRange(resp)) {
            return getNotSupportRangeType(resp, url);
        } else {
            return getRangeSupportedType(resp, url);
        }
    }

    private DownloadType getRangeSupportedType(Response<Void> resp, String url) {
        long contentLength = contentLength(resp);
        try {
            if (needReDownload(url, contentLength)) {
                return type.multithread(url, contentLength, lastModify(resp));
            }
            if (downloadNotComplete(url)) {
                return type.continued(url, contentLength, lastModify(resp));
            }
        } catch (IOException e) {
            log(DOWNLOAD_RECORD_FILE_DAMAGED);
            return type.multithread(url, contentLength, lastModify(resp));
        }
        return type.already(contentLength);
    }

    private DownloadType getNotSupportRangeType(Response<Void> resp, String url) {
        long contentLength = contentLength(resp);
        if (downloadNotComplete(url, contentLength)) {
            return type.normal(url, contentLength, lastModify(resp));
        } else {
            return type.already(contentLength);
        }
    }
}
