package zlc.season.rxdownload;

import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static zlc.season.rxdownload.DownloadHelper.TAG;


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
            Log.i(TAG, "Normal download start!!");
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
                            return mDownloadHelper.retry(integer, throwable);
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
                            return mDownloadHelper.retry(integer, throwable);
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
            Log.i(TAG, "Continue download start!!");
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
            Log.i(TAG, "Multi Thread download start!!");
            return super.startDownload();
        }
    }

    static class AlreadyDownloaded extends DownloadType {

        @Override
        void prepareDownload() throws IOException, ParseException {
            Log.i(TAG, "File Already downloaded!!");
        }

        @Override
        Observable<DownloadStatus> startDownload() throws IOException {
            return Observable.just(new DownloadStatus(mFileLength, mFileLength));
        }
    }
}
