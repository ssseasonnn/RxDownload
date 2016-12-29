package zlc.season.rxdownload2.function;

import android.content.Context;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.FlowableEmitter;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import zlc.season.rxdownload2.entity.DownloadRange;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownload2.entity.DownloadType;
import zlc.season.rxdownload2.entity.DownloadTypeFactory;

import static android.text.TextUtils.concat;
import static zlc.season.rxdownload2.function.FileHelper.TAG;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:39
 * Download helper 类
 */
public class DownloadHelper {
    private static final String TEST_RANGE_SUPPORT = "bytes=0-";

    private int MAX_RETRY_COUNT = 3;

    private DownloadApi mDownloadApi;
    private FileHelper mFileHelper;
    private DownloadTypeFactory mFactory;

    //Record : { "url" : new String[] { "file path" , "temp file path" , "last modify file path" }}
    private Map<String, String[]> mDownloadRecord;

    public DownloadHelper() {
        mDownloadRecord = new HashMap<>();
        mFileHelper = new FileHelper();
        mDownloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
        mFactory = new DownloadTypeFactory(this);
    }

    public void setRetrofit(Retrofit retrofit) {
        mDownloadApi = retrofit.create(DownloadApi.class);
    }

    public void setDefaultSavePath(String defaultSavePath) {
        mFileHelper.setDefaultSavePath(defaultSavePath);
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

    public Boolean retry(Integer integer, Throwable throwable) {
        if (throwable instanceof ProtocolException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(FileHelper.TAG, Thread.currentThread().getName() +
                        " we got an error in the underlying protocol, such as a TCP error, retry to connect " +
                        integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof UnknownHostException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(FileHelper.TAG, Thread.currentThread().getName() +
                        " no network, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof HttpException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(FileHelper.TAG, Thread.currentThread().getName() +
                        " had non-2XX http error, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof SocketTimeoutException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(FileHelper.TAG, Thread.currentThread().getName() +
                        " socket time out,retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof ConnectException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(FileHelper.TAG, concat(Thread.currentThread().getName(), " ", throwable.getMessage(),
                        ". retry to connect ", String.valueOf(integer), " times").toString());
                return true;
            }
            return false;
        } else if (throwable instanceof SocketException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(FileHelper.TAG, Thread.currentThread().getName() +
                        " a network or conversion error happened, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof CompositeException) {
            Log.w(FileHelper.TAG, throwable.getMessage());
            return false;
        } else {
            Log.w(FileHelper.TAG, throwable);
            return false;
        }
    }

    public int getMaxThreads() {
        return mFileHelper.getMaxThreads();
    }

    public void setMaxThreads(int MAX_THREADS) {
        mFileHelper.setMaxThreads(MAX_THREADS);
    }

    public void prepareNormalDownload(String url, long fileLength, String lastModify) throws IOException,
            ParseException {
        mFileHelper.prepareDownload(getLastModifyFile(url), getFile(url), fileLength, lastModify);
    }

    public void saveNormalFile(FlowableEmitter<DownloadStatus> emitter, String url, Response<ResponseBody> resp) {
        mFileHelper.saveFile(emitter, getFile(url), resp);
    }

    public DownloadRange readDownloadRange(String url, int i) throws IOException {
        return mFileHelper.readDownloadRange(getTempFile(url), i);
    }

    public void prepareMultiThreadDownload(String url, long fileLength, String lastModify) throws IOException,
            ParseException {
        mFileHelper.prepareDownload(getLastModifyFile(url), getTempFile(url), getFile(url),
                fileLength, lastModify);
    }

    public void saveRangeFile(FlowableEmitter<DownloadStatus> emitter, int i, long start, long end,
                              String url, ResponseBody response) {
        mFileHelper.saveFile(emitter, i, start, end, getTempFile(url), getFile(url), response);
    }

    public Observable<DownloadStatus> downloadDispatcher(final String url, final String saveName,
                                                         final String savePath, final Context context,
                                                         final boolean autoInstall) {
        if (isRecordExists(url)) {
            return Observable.error(new Throwable("This url download task already exists, so do nothing."));
        }
        try {
            addDownloadRecord(url, saveName, savePath);
        } catch (IOException e) {
            return Observable.error(e);
        }
        return getDownloadType(url)
                .flatMap(new Function<DownloadType, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(DownloadType downloadType) throws Exception {
                        downloadType.prepareDownload();
                        return downloadType.startDownload();
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        if (autoInstall) {
                            if (context == null) {
                                throw new IllegalStateException("Context is NULL! You should call " +
                                        "#RxDownload.context(Context context)# first!");
                            }
                            Utils.installApk(context, new File(getRealFilePaths(saveName, savePath)[0]));
                        }
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (throwable instanceof CompositeException) {
                            Log.w(TAG, throwable.getMessage());
                        } else {
                            Log.w(TAG, throwable);
                        }
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        deleteDownloadRecord(url);
                    }
                });
    }

    public Observable<DownloadType> requestHeaderWithIfRangeByGet(final String url) throws IOException {
        return getDownloadApi()
                .requestWithIfRange(TEST_RANGE_SUPPORT, getLastModify(url), url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> response) throws Exception {
                        if (Utils.serverFileNotChange(response)) {
                            return getWhenServerFileNotChange(response, url);
                        } else if (Utils.serverFileChanged(response)) {
                            return getWhenServerFileChanged(response, url);
                        } else {
                            throw new RuntimeException("unknown error");
                        }
                    }
                }).retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(Integer integer, Throwable throwable) throws Exception {
                        return retry(integer, throwable);
                    }
                });
    }

    private void addDownloadRecord(String url, String saveName, String savePath) throws IOException {
        mFileHelper.createDirectories(savePath);
        mDownloadRecord.put(url, getRealFilePaths(saveName, savePath));
    }

    private boolean isRecordExists(String url) {
        return mDownloadRecord.get(url) != null;
    }

    private void deleteDownloadRecord(String url) {
        mDownloadRecord.remove(url);
    }

    private String getLastModify(String url) throws IOException {
        return mFileHelper.getLastModify(getLastModifyFile(url));
    }

    private boolean downloadNotComplete(String url) throws IOException {
        return mFileHelper.downloadNotComplete(getTempFile(url));
    }

    private boolean downloadNotComplete(String url, long contentLength) {
        return getFile(url).length() != contentLength;
    }

    private boolean needReDownload(String url, long contentLength) throws IOException {
        return tempFileNotExists(url) || tempFileDamaged(url, contentLength);
    }

    private boolean downloadFileExists(String url) {
        return getFile(url).exists();
    }

    private boolean tempFileDamaged(String url, long fileLength) throws IOException {
        return mFileHelper.tempFileDamaged(getTempFile(url), fileLength);
    }

    private boolean tempFileNotExists(String url) {
        return !getTempFile(url).exists();
    }

    private File getFile(String url) {
        return new File(mDownloadRecord.get(url)[0]);
    }

    private File getTempFile(String url) {
        return new File(mDownloadRecord.get(url)[1]);
    }

    private File getLastModifyFile(String url) {
        return new File(mDownloadRecord.get(url)[2]);
    }

    private Observable<DownloadType> getDownloadType(String url) {
        if (downloadFileExists(url)) {
            try {
                return getWhenFileExists(url);
            } catch (IOException e) {
                return getWhenFileNotExists(url);
            }
        } else {
            return getWhenFileNotExists(url);
        }
    }

    private Observable<DownloadType> getWhenFileNotExists(final String url) {
        return getDownloadApi()
                .getHttpHeader(TEST_RANGE_SUPPORT, url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> response) throws Exception {
                        if (Utils.notSupportRange(response)) {
                            return mFactory.url(url)
                                    .fileLength(Utils.contentLength(response))
                                    .lastModify(Utils.lastModify(response))
                                    .buildNormalDownload();
                        } else {
                            return mFactory.url(url)
                                    .lastModify(Utils.lastModify(response))
                                    .fileLength(Utils.contentLength(response))
                                    .buildMultiDownload();
                        }
                    }
                })
                .retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(Integer integer, Throwable throwable) throws Exception {
                        return retry(integer, throwable);
                    }
                });

    }

    private Observable<DownloadType> getWhenFileExists(final String url) throws IOException {
        return getDownloadApi()
                .getHttpHeaderWithIfRange(TEST_RANGE_SUPPORT, getLastModify(url), url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> resp) throws Exception {
                        if (Utils.serverFileNotChange(resp)) {
                            return getWhenServerFileNotChange(resp, url);
                        } else if (Utils.serverFileChanged(resp)) {
                            return getWhenServerFileChanged(resp, url);
                        } else if (Utils.requestRangeNotSatisfiable(resp)) {
                            return mFactory.url(url)
                                    .fileLength(Utils.contentLength(resp))
                                    .lastModify(Utils.lastModify(resp))
                                    .buildRequestRangeNotSatisfiable();
                        } else {
                            throw new RuntimeException("unknown error");
                        }
                    }
                })
                .retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(Integer integer, Throwable throwable) throws Exception {
                        return retry(integer, throwable);
                    }
                });
    }

    private DownloadType getWhenServerFileChanged(Response<Void> resp, String url) {
        if (Utils.notSupportRange(resp)) {
            return mFactory.url(url)
                    .fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp))
                    .buildNormalDownload();
        } else {
            return mFactory.url(url)
                    .fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp))
                    .buildMultiDownload();
        }
    }

    private DownloadType getWhenServerFileNotChange(Response<Void> resp, String url) {
        if (Utils.notSupportRange(resp)) {
            return getWhenNotSupportRange(resp, url);
        } else {
            return getWhenSupportRange(resp, url);
        }
    }

    private DownloadType getWhenSupportRange(Response<Void> resp, String url) {
        long contentLength = Utils.contentLength(resp);
        try {
            if (needReDownload(url, contentLength)) {
                return mFactory.url(url)
                        .fileLength(contentLength)
                        .lastModify(Utils.lastModify(resp))
                        .buildMultiDownload();
            }
            if (downloadNotComplete(url)) {
                return mFactory.url(url)
                        .fileLength(contentLength)
                        .lastModify(Utils.lastModify(resp))
                        .buildContinueDownload();
            }
        } catch (IOException e) {
            Log.w(TAG, "download record file may be damaged,so we will re download");
            return mFactory.url(url)
                    .fileLength(contentLength)
                    .lastModify(Utils.lastModify(resp))
                    .buildMultiDownload();
        }
        return mFactory.fileLength(contentLength).buildAlreadyDownload();
    }

    private DownloadType getWhenNotSupportRange(Response<Void> resp, String url) {
        long contentLength = Utils.contentLength(resp);
        if (downloadNotComplete(url, contentLength)) {
            return mFactory.url(url)
                    .fileLength(contentLength)
                    .lastModify(Utils.lastModify(resp))
                    .buildNormalDownload();
        } else {
            return mFactory.fileLength(contentLength).buildAlreadyDownload();
        }
    }
}
