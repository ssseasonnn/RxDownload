package zlc.season.rxdownload.util;

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
import zlc.season.rxdownload.entity.DownloadRange;
import zlc.season.rxdownload.entity.DownloadStatus;

import static android.text.TextUtils.concat;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:39
 * Download helper 类
 */
public class DownloadHelper {
    public static final String TEST_RANGE_SUPPORT = "bytes=0-";

    private int MAX_RETRY_COUNT = 3;

    private DownloadApi mDownloadApi;
    private FileHelper mFileHelper;

    //Record : { "url" : new String[] { "file path" , "temp file path" , "last modify file path" }}
    private Map<String, String[]> mDownloadRecord;

    public DownloadHelper() {
        mDownloadRecord = new HashMap<>();
        mFileHelper = new FileHelper();
        mDownloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
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

    public void addDownloadRecord(String url, String saveName, String savePath) throws IOException {
        mFileHelper.createDirectories(savePath);
        mDownloadRecord.put(url, mFileHelper.getRealFilePaths(saveName, savePath));
    }

    public void deleteDownloadRecord(String url) {
        mDownloadRecord.remove(url);
    }

    public String getLastModify(String url) throws IOException {
        return mFileHelper.getLastModify(getLastModifyFileBy(url));
    }

    public boolean downloadNotComplete(String url) throws IOException {
        return mFileHelper.downloadNotComplete(getTempFileBy(url));
    }

    public boolean downloadNotComplete(String url, long contentLength) {
        return getFileBy(url).length() != contentLength;
    }

    public boolean needReDownload(String url, long contentLength) throws IOException {
        return tempFileNotExists(url) || tempFileDamaged(url, contentLength);
    }

    public String[] getFileSavePaths(String savePath) {
        return mFileHelper.getRealDirectoryPaths(savePath);
    }

    public boolean downloadFileExists(String url) {
        return getFileBy(url).exists();
    }

    int getMaxThreads() {
        return mFileHelper.getMaxThreads();
    }

    public void setMaxThreads(int MAX_THREADS) {
        mFileHelper.setMaxThreads(MAX_THREADS);
    }

    public   DownloadApi getDownloadApi() {
        return mDownloadApi;
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

    public Boolean retry(Integer integer, Throwable throwable) {
        if (throwable instanceof UnknownHostException) {
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
