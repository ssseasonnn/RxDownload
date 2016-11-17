package zlc.season.rxdownload;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.HttpException;
import rx.Subscriber;
import rx.exceptions.CompositeException;

import static android.text.TextUtils.concat;
import static zlc.season.rxdownload.FileHelper.TAG;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:39
 * Download helper 类
 */
class DownloadHelper {
    static final String TEST_RANGE_SUPPORT = "bytes=0-";

    private int MAX_RETRY_COUNT = 3;

    private DownloadApi mDownloadApi;
    private FileHelper mFileHelper;

    //Record : { "url" : new String[] { "file path" , "temp file path" , "last modify file path" }}
    private Map<String, String[]> mDownloadRecord;

    DownloadHelper() {
        mDownloadRecord = new HashMap<>();
        mFileHelper = new FileHelper();
        mDownloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
    }

    void setRetrofit(Retrofit retrofit) {
        mDownloadApi = retrofit.create(DownloadApi.class);
    }

    void setDefaultSavePath(String defaultSavePath) {
        mFileHelper.setDefaultSavePath(defaultSavePath);
    }

    int getMaxThreads() {
        return mFileHelper.getMaxThreads();
    }

    void setMaxThreads(int MAX_THREADS) {
        mFileHelper.setMaxThreads(MAX_THREADS);
    }

    void setMaxRetryCount(int MAX_RETRY_COUNT) {
        this.MAX_RETRY_COUNT = MAX_RETRY_COUNT;
    }

    DownloadApi getDownloadApi() {
        return mDownloadApi;
    }

    void addDownloadRecord(String url, String saveName, String savePath) throws IOException {
        mFileHelper.createDirectories(savePath);
        mDownloadRecord.put(url, mFileHelper.getRealFilePaths(saveName, savePath));
    }

    void deleteDownloadRecord(String url) {
        mDownloadRecord.remove(url);
    }

    String getLastModify(String url) throws IOException {
        return mFileHelper.getLastModify(getLastModifyFileBy(url));
    }

    void prepareNormalDownload(String url, long fileLength, String lastModify) throws IOException, ParseException {
        mFileHelper.prepareDownload(getLastModifyFileBy(url), getFileBy(url), fileLength, lastModify);
    }

    void saveNormalFile(Subscriber<? super DownloadStatus> sub, String url, Response<ResponseBody> resp) {
        mFileHelper.saveFile(sub, getFileBy(url), resp);
    }

    DownloadRange readDownloadRange(String url) throws IOException {
        return mFileHelper.readDownloadRange(getTempFileBy(url));
    }

    void prepareMultiThreadDownload(String url, long fileLength, String lastModify) throws IOException, ParseException {
        mFileHelper.prepareDownload(getLastModifyFileBy(url), getTempFileBy(url), getFileBy(url),
                fileLength, lastModify);
    }

    void saveRangeFile(Subscriber<? super DownloadStatus> subscriber, int i, long start, long end,
                       String url, ResponseBody response) {
        mFileHelper.saveFile(subscriber, i, start, end, getTempFileBy(url), getFileBy(url), response);
    }

    boolean downloadNotComplete(String url) throws IOException {
        return mFileHelper.downloadNotComplete(getTempFileBy(url));
    }

    boolean downloadNotComplete(String url, long contentLength) {
        return getFileBy(url).length() != contentLength;
    }

    boolean needReDownload(String url, long contentLength) throws IOException {
        return tempFileNotExists(url) || tempFileDamaged(url, contentLength);
    }

    String[] getFileSavePaths(String savePath) {
        return mFileHelper.getRealDirectoryPaths(savePath);
    }

    Boolean retry(Integer integer, Throwable throwable) {
        if (throwable instanceof UnknownHostException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " no network, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof HttpException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " had non-2XX http error, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof SocketTimeoutException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " socket time out,retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof ConnectException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, concat(Thread.currentThread().getName(), " ", throwable.getMessage(),
                        ". retry to connect ", String.valueOf(integer), " times").toString());
                return true;
            }
            return false;
        } else if (throwable instanceof SocketException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " a network or conversion error happened, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof CompositeException) {
            Log.w(TAG, throwable.getMessage());
            return false;
        } else {
            Log.w(TAG, throwable);
            return false;
        }
    }

    boolean downloadFileExists(String url) {
        return getFileBy(url).exists();
    }

    private boolean tempFileDamaged(String url, long fileLength) throws IOException {
        return mFileHelper.tempFileDamaged(getTempFileBy(url), fileLength);
    }

    private boolean tempFileNotExists(String url) {
        return !getTempFileBy(url).exists();
    }

    private File getFileBy(String url) {
        return new File(mDownloadRecord.get(url)[0]);
    }

    private File getTempFileBy(String url) {
        return new File(mDownloadRecord.get(url)[1]);
    }

    private File getLastModifyFileBy(String url) {
        return new File(mDownloadRecord.get(url)[2]);
    }
}
