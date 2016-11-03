package zlc.season.rxdownload;

import android.util.Log;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.exceptions.CompositeException;

import static android.content.ContentValues.TAG;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 09:44
 * FIXME
 */
abstract class DownloadType {
    String mUrl;
    long mFileLength;
    String mLastModify;
    FileHelper mFileHelper;

    abstract void prepareDownload() throws IOException, ParseException;

    abstract Observable<DownloadStatus> startDownload() throws IOException;

    Boolean retry(Integer integer, Throwable throwable) {
        int MAX_RETRY_COUNT = mFileHelper.getMaxRetryCount();
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

    static class Builder {
        private String mUrl;
        private long mFileLength;
        private String mLastModify;
        private FileHelper mFileHelper;

        Builder(FileHelper fileHelper) {
            this.mFileHelper = fileHelper;
        }

        Builder url(String url) {
            this.mUrl = url;
            return this;
        }

        Builder fileLength(long fileLength) {
            this.mFileLength = fileLength;
            return this;
        }

        Builder lastModify(String lastModify) {
            this.mLastModify = lastModify;
            return this;
        }

        DownloadType buildNormalDownload() {
            DownloadType type = new NormalDownload();
            type.mUrl = this.mUrl;
            type.mFileLength = this.mFileLength;
            type.mLastModify = this.mLastModify;
            type.mFileHelper = this.mFileHelper;
            return type;
        }

        DownloadType buildContinueDownload() {
            DownloadType type = new ContinueDownload();
            type.mUrl = this.mUrl;
            type.mFileLength = this.mFileLength;
            type.mLastModify = this.mLastModify;
            type.mFileHelper = this.mFileHelper;
            return type;
        }

        DownloadType buildMultiDownload() {
            DownloadType type = new MultiThreadDownload();
            type.mUrl = this.mUrl;
            type.mFileLength = this.mFileLength;
            type.mLastModify = this.mLastModify;
            type.mFileHelper = this.mFileHelper;
            return type;
        }

        DownloadType buildAlreadyDownload() {
            DownloadType type = new AlreadyDownloaded();
            type.mUrl = this.mUrl;
            type.mFileLength = this.mFileLength;
            type.mLastModify = this.mLastModify;
            type.mFileHelper = this.mFileHelper;
            return type;
        }
    }
}
