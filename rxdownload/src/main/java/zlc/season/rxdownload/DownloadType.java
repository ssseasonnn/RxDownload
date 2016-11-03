package zlc.season.rxdownload;

import android.util.Log;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static android.content.ContentValues.TAG;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 09:44
 * 下载类型
 */
abstract class DownloadType {
    String mUrl;
    long mFileLength;
    String mLastModify;
    DownloadHelper mDownloadHelper;

    abstract void prepareDownload() throws IOException, ParseException;

    abstract Observable<DownloadStatus> startDownload() throws IOException;

    Boolean retry(Integer integer, Throwable throwable) {
        int MAX_RETRY_COUNT = mDownloadHelper.getMaxRetryCount();
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

    static class NormalDownload extends DownloadType {

        @Override
        void prepareDownload() throws IOException, ParseException {
            mDownloadHelper.prepareNormalDownload(mUrl, mFileLength, mLastModify);
        }

        private Observable<DownloadStatus> normalSave(final Response<ResponseBody> response) {
            return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
                @Override
                public void call(Subscriber<? super DownloadStatus> subscriber) {
                    mDownloadHelper.saveNormalFile(subscriber, mUrl, response);
                }
            });
        }

        @Override
        Observable<DownloadStatus> startDownload() {
            return mDownloadHelper.getDownloadApi().download(null, mUrl)
                    .subscribeOn(Schedulers.io())
                    .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                        @Override
                        public Observable<DownloadStatus> call(final Response<ResponseBody> response) {
                            return normalSave(response);
                        }
                    }).onBackpressureLatest().retry(new Func2<Integer, Throwable, Boolean>() {
                        @Override
                        public Boolean call(Integer integer, Throwable throwable) {
                            return retry(integer, throwable);
                        }
                    });
        }
    }

    static class ContinueDownload extends DownloadType {

        /**
         * 分段下载的任务
         *
         * @param start 从start字节开始
         * @param end   到end字节结束
         * @param i     下载编号
         * @return Observable
         */
        private Observable<DownloadStatus> rangeDownloadTask(final long start, final long end, final int i) {
            String range = "bytes=" + start + "-" + end;
            return mDownloadHelper.getDownloadApi().download(range, mUrl)
                    .subscribeOn(Schedulers.io())
                    .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                        @Override
                        public Observable<DownloadStatus> call(Response<ResponseBody> response) {
                            return rangeSave(start, end, i, response.body());
                        }
                    }).onBackpressureLatest().retry(new Func2<Integer, Throwable, Boolean>() {
                        @Override
                        public Boolean call(Integer integer, Throwable throwable) {
                            return retry(integer, throwable);
                        }
                    });
        }

        /**
         * 保存断点下载的文件,以及下载进度
         *
         * @param start    从start开始
         * @param end      到end结束
         * @param i        下载编号
         * @param response 响应值
         * @return Observable
         */
        private Observable<DownloadStatus> rangeSave(final long start, final long end, final int i, final ResponseBody
                response) {
            return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
                @Override
                public void call(Subscriber<? super DownloadStatus> subscriber) {
                    mDownloadHelper.saveRangeFile(subscriber, i, start, end, mUrl, response);
                }
            });
        }

        @Override
        void prepareDownload() throws IOException, ParseException {
        }

        @Override
        Observable<DownloadStatus> startDownload() throws IOException {
            DownloadRange range = mDownloadHelper.getDownloadRange(mUrl);
            List<Observable<DownloadStatus>> tasks = new ArrayList<>();
            for (int i = 0; i < mDownloadHelper.getMaxThreads(); i++) {
                if (range.start[i] <= range.end[i]) {
                    tasks.add(rangeDownloadTask(range.start[i], range.end[i], i));
                }
            }
            return Observable.mergeDelayError(tasks);
        }
    }

    static class MultiThreadDownload extends ContinueDownload {

        @Override
        void prepareDownload() throws IOException, ParseException {
            mDownloadHelper.prepareMultiThreadDownload(mUrl, mFileLength, mLastModify);
        }

        @Override
        Observable<DownloadStatus> startDownload() throws IOException {
            return super.startDownload();
        }
    }

    static class AlreadyDownloaded extends DownloadType {

        @Override
        void prepareDownload() throws IOException, ParseException {

        }

        @Override
        Observable<DownloadStatus> startDownload() throws IOException {
            return Observable.just(new DownloadStatus(mFileLength, mFileLength));
        }
    }
}
