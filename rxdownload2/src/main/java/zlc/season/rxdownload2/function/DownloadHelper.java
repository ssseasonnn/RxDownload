package zlc.season.rxdownload2.function;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import io.reactivex.FlowableEmitter;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
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

import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_URL_EXISTS;
import static zlc.season.rxdownload2.function.Constant.TEST_RANGE_SUPPORT;
import static zlc.season.rxdownload2.function.Constant.TEST_RANGE_SUPPORT_BY_GET;
import static zlc.season.rxdownload2.function.Utils.empty;
import static zlc.season.rxdownload2.function.Utils.log;
import static zlc.season.rxdownload2.function.Utils.retry;
import static zlc.season.rxdownload2.function.Utils.serverFileChanged;
import static zlc.season.rxdownload2.function.Utils.serverFileNotChange;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:39
 * Download helper
 */
public class DownloadHelper {
    private int MAX_RETRY_COUNT = 3;

    private DownloadApi downloadApi;
    private FileHelper fileHelper;

    private DownloadTypeFactory type;
    private TemporaryRecordTable recordTable;

    public DownloadHelper() {
        fileHelper = new FileHelper();
        downloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
        type = new DownloadTypeFactory(this);

        recordTable = new TemporaryRecordTable(downloadHelper);
    }

    public void setRetrofit(Retrofit retrofit) {
        downloadApi = retrofit.create(DownloadApi.class);
    }

    public void setDefaultSavePath(String defaultSavePath) {
        fileHelper.setDefaultSavePath(defaultSavePath);
    }

    public int getMaxRetryCount() {
        return MAX_RETRY_COUNT;
    }

    public void setMaxRetryCount(int MAX_RETRY_COUNT) {
        this.MAX_RETRY_COUNT = MAX_RETRY_COUNT;
    }

    public String[] getFileSavePaths(String savePath) {
        return fileHelper.getRealDirectoryPaths(savePath);
    }

    public String[] getRealFilePaths(String saveName, String savePath) {
        return fileHelper.getRealFilePaths(saveName, savePath);
    }

    public DownloadApi getDownloadApi() {
        return downloadApi;
    }

    public int getMaxThreads() {
        return fileHelper.getMaxThreads();
    }

    public void setMaxThreads(int MAX_THREADS) {
        fileHelper.setMaxThreads(MAX_THREADS);
    }

    public void prepareNormalDownload(String url, long fileLength, String lastModify)
            throws IOException, ParseException {

        fileHelper.prepareDownload(getLastModifyFile(url),
                getFile(url), fileLength, lastModify);
    }

    public void saveNormalFile(FlowableEmitter<DownloadStatus> emitter,
                               String url, Response<ResponseBody> resp) {

        fileHelper.saveFile(emitter, getFile(url), resp);
    }

    public DownloadRange readDownloadRange(String url, int i)
            throws IOException {
        return fileHelper.readDownloadRange(getTempFile(url), i);
    }

    public void prepareMultiThreadDownload(String url, long fileLength, String lastModify)
            throws IOException, ParseException {

        fileHelper.prepareDownload(getLastModifyFile(url), getTempFile(url), getFile(url),
                fileLength, lastModify);
    }

    /**
     * This method saves the file from the {start} to {end} range.
     *
     * @param emitter  emitter
     * @param i        index
     * @param start    from
     * @param end      end
     * @param url      url
     * @param response response
     */
    public void saveRangeFile(FlowableEmitter<DownloadStatus> emitter,
                              int i, long start, long end, String url,
                              ResponseBody response) {

        fileHelper.saveFile(emitter, i, start, end,
                getTempFile(url), getFile(url), response);
    }

    /**
     * dispatch download
     *
     * @param url      url for download
     * @param saveName save name
     * @param savePath save path
     * @return DownloadStatus
     */
    public Observable<DownloadStatus> downloadDispatcher(final String url,
                                                         final String saveName,
                                                         final String savePath) {
        return Observable.just(1)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        beforeDownload(url, saveName, savePath);
                    }
                })
                .flatMap(new Function<Integer, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Integer integer) throws Exception {
                        return getDownloadType(url);
                    }
                })
                .flatMap(new Function<DownloadType, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(DownloadType downloadType)
                            throws Exception {
                        downloadType.prepareDownload();
                        return downloadType.startDownload();
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
     * some server such IIS does not support checkRangeByHead method, use GET.
     *
     * @param url url
     * @return DownloadType
     * @throws IOException
     */
    public Observable<DownloadType> notSupportHead(final String url)
            throws IOException {

        return downloadApi
                .GET(TEST_RANGE_SUPPORT, readLastModify(url), url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> response)
                            throws Exception {
                        if (serverFileNotChange(response)) {
                            return getFileNotChangeType(response, url);
                        } else if (serverFileChanged(response)) {
                            return getFileChangedType(response, url);
                        } else {
                            return type.unable();
                        }
                    }
                })
                .compose(Utils.<DownloadType>retry(MAX_RETRY_COUNT));
    }

    private void beforeDownload(String url, String saveName, String savePath)
            throws IOException {

        if (recordTable.contain(url)) {
            throw new IOException(DOWNLOAD_URL_EXISTS);
        }
        fileHelper.createDownloadDirs(savePath);

        addTempRecord(url, saveName, savePath);
    }

    /**
     * Add a temporary record to the record recordTable. Only for temporary record.
     *
     * @param url      temp record url.
     * @param saveName temp record saveName, maybe empty.
     * @param savePath temp record savePath
     */
    private void addTempRecord(String url, String saveName, String savePath) {
        recordTable.add(url, new TemporaryRecord(url, saveName, savePath));
    }

    private String readLastModify(String url) throws IOException {
        return fileHelper.getLastModify(getLastModifyFile(url));
    }

    public boolean downloadNotComplete(String url) throws IOException {
        return fileHelper.downloadNotComplete(getTempFile(url));
    }

    public boolean downloadNotComplete(String url, long contentLength) {
        return getFile(url).length() != contentLength;
    }

    public boolean needReDownload(String url, long contentLength) throws IOException {
        return tempFileNotExists(url) || tempFileDamaged(url, contentLength);
    }

    private boolean downloadFileExists(String url) {
        return getFile(url).exists();
    }

    private boolean tempFileDamaged(String url, long fileLength)
            throws IOException {
        return fileHelper.tempFileDamaged(getTempFile(url), fileLength);
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
     * get download type.
     *
     * @param url url
     * @return download type
     */
    private Observable<DownloadType> getDownloadType(final String url) {
        return Observable.just(1)
                .flatMap(new Function<Integer, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Integer integer) throws Exception {
                        return checkRange(url);
                    }
                })
                .flatMap(new Function<Object, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Object o) throws Exception {
                        if (downloadFileExists(url)) {
                            return getFileExistsType(url);
                        } else {
                            return getFileNotExistsType(url);
                        }
                    }
                });
    }

    /**
     * Gets the download type of file non-existence.
     *
     * @param url file url
     * @return Download Type
     */
    private Observable<DownloadType> getFileNotExistsType(final String url) {
        return Observable.just(1)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        recordTable.setFileNotExists(url);
                    }
                })
                .flatMap(new Function<Integer, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Integer integer) throws Exception {
                        if (recordTable.notSupportHeadMethod(url)) {
                            return queryByGet(url);
                        }
                        return Observable.just(new Object());
                    }
                })
                .flatMap(new Function<Object, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Object o) throws Exception {
                        recordTable.generateFileNotExistsType(url);
                        return Observable.just(recordTable.getType(url));
                    }
                });
    }

    /**
     * Gets the download type of file existence.
     *
     * @param url file url
     * @return Download Type
     */
    private Observable<DownloadType> getFileExistsType(final String url) {
        return Observable.just(1)
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        String lmf;
                        try {
                            lmf = readLastModify(url);
                        } catch (IOException e) {
                            lmf = "";
                        }
                        return lmf;
                    }
                })
                .flatMap(new Function<String, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(String s) throws Exception {
                        //First of all this file contain.
                        recordTable.setFileExists(url);
                        if (empty(s)) {
                            //Read LastModify failure, the file may have been deleted,
                            // or what other reasons can not be read correctly.
                            recordTable.setLastModifyReadFailed(url);
                        } else {
                            //LastModify read success, so the next check whether the file changes.
                            recordTable.setLastModifyReadSuccess(url);
                            return query(url, s);
                        }
                        return Observable.just(new Object());
                    }
                })
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        recordTable.generateFileExistsType(url);
                    }
                })
                .flatMap(new Function<Object, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Object o) throws Exception {
                        return Observable.just(recordTable.getType(url));
                    }
                });
    }


    /**
     * http checkRangeByHead request,checkRange need info.
     *
     * @param url url
     * @return empty Observable
     */
    private ObservableSource<Object> checkRange(final String url) {
        return downloadApi.checkRangeByHead(TEST_RANGE_SUPPORT, url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(Response<Void> response) throws Exception {
                        recordTable.saveHttpInfo(url, response);
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(Response<Void> response) throws Exception {
                        return new Object();
                    }
                })
                .compose(retry(MAX_RETRY_COUNT));
    }

    private ObservableSource<Object> checkFile(final String url, String lastModify) {
        return downloadApi.checkFileByHead(lastModify, url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(Response<Void> response) throws Exception {
                        recordTable.saveFileState(url, response);
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(Response<Void> response) throws Exception {
                        return new Object();
                    }
                })
                .compose(retry(MAX_RETRY_COUNT));
    }

    private ObservableSource<Object> queryByGet(final String url) {
        return downloadApi.GET(TEST_RANGE_SUPPORT_BY_GET, url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(Response<Void> response) throws Exception {
                        recordTable.saveHttpInfo(url, response);
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(Response<Void> response) throws Exception {
                        return new Object();
                    }
                })
                .compose(retry(MAX_RETRY_COUNT));
    }

    /**
     * http checkRangeByHead request,checkRange need info, check whether if server file has changed.
     *
     * @param url url
     * @return empty Observable
     */
    private ObservableSource<Object> query(final String url, String lastModify) {
        return downloadApi.checkRangeByHead(TEST_RANGE_SUPPORT, lastModify)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(Response<Void> response) throws Exception {
                        recordTable.updateExtraInfo(url, response);
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(Response<Void> response) throws Exception {
                        return new Object();
                    }
                })
                .compose(retry(MAX_RETRY_COUNT));
    }
}
