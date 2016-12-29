package zlc.season.rxdownload.entity;

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
import zlc.season.rxdownload.function.DownloadHelper;
import zlc.season.rxdownload.function.FileHelper;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 09:44
 * 下载类型
 */
public abstract class DownloadType {
    String mUrl;
    long mFileLength;
    String mLastModify;
    DownloadHelper mDownloadHelper;

    public abstract void prepareDownload() throws IOException, ParseException;

    public abstract Observable<DownloadStatus> startDownload() throws IOException;

    static class NormalDownload extends DownloadType {

        @Override
        public void prepareDownload() throws IOException, ParseException {
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
        public Observable<DownloadStatus> startDownload() {
            Log.i(FileHelper.TAG, "Normal download start!!");
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
         * @param i 下载编号
         * @return Observable
         */
        private Observable<DownloadStatus> rangeDownloadTask(final int i) {
            return Observable.create(new Observable.OnSubscribe<DownloadRange>() {
                @Override
                public void call(Subscriber<? super DownloadRange> subscriber) {
                    try {
                        DownloadRange range = mDownloadHelper.readDownloadRange(mUrl, i);
                        if (range.start <= range.end) {
                            subscriber.onNext(range);
                        }
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
            }).subscribeOn(Schedulers.io()).flatMap(new Func1<DownloadRange, Observable<DownloadStatus>>() {
                @Override
                public Observable<DownloadStatus> call(final DownloadRange downloadRange) {
                    String range = "bytes=" + downloadRange.start + "-" + downloadRange.end;
                    return mDownloadHelper.getDownloadApi().download(range, mUrl)
                            .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                                @Override
                                public Observable<DownloadStatus> call(Response<ResponseBody> response) {
                                    return rangeSave(downloadRange.start, downloadRange.end, i, response.body());
                                }
                            });
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
        private Observable<DownloadStatus> rangeSave(final long start, final long end, final int i,
                                                     final ResponseBody response) {
            return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
                @Override
                public void call(Subscriber<? super DownloadStatus> subscriber) {
                    mDownloadHelper.saveRangeFile(subscriber, i, start, end, mUrl, response);
                }
            });
        }

        @Override
        public void prepareDownload() throws IOException, ParseException {
            Log.i(FileHelper.TAG, "Continue download start!!");
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            List<Observable<DownloadStatus>> tasks = new ArrayList<>();
            for (int i = 0; i < mDownloadHelper.getMaxThreads(); i++) {
                tasks.add(rangeDownloadTask(i));
            }
            return Observable.mergeDelayError(tasks);
        }
    }

    static class MultiThreadDownload extends ContinueDownload {

        @Override
        public void prepareDownload() throws IOException, ParseException {
            mDownloadHelper.prepareMultiThreadDownload(mUrl, mFileLength, mLastModify);
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            Log.i(FileHelper.TAG, "Multi Thread download start!!");
            return super.startDownload();
        }
    }

    static class AlreadyDownloaded extends DownloadType {

        @Override
        public void prepareDownload() throws IOException, ParseException {
            Log.i(FileHelper.TAG, "File Already downloaded!!");
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            return Observable.just(new DownloadStatus(mFileLength, mFileLength));
        }
    }

    static class RequestRangeNotSatisfiable extends DownloadType {

        @Override
        public void prepareDownload() throws IOException, ParseException {

        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            return mDownloadHelper.requestHeaderWithIfRangeByGet(mUrl)
                    .flatMap(new Func1<DownloadType, Observable<DownloadStatus>>() {
                        @Override
                        public Observable<DownloadStatus> call(DownloadType type) {
                            try {
                                type.prepareDownload();
                                return type.startDownload();
                            } catch (IOException | ParseException e) {
                                return Observable.error(e);
                            }
                        }
                    });
        }
    }
}
