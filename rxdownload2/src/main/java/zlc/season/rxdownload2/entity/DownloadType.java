package zlc.season.rxdownload2.entity;

import android.util.Log;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;
import zlc.season.rxdownload2.function.DownloadHelper;
import zlc.season.rxdownload2.function.FileHelper;


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

        private Flowable<DownloadStatus> normalSave(final Response<ResponseBody> response) {
            return Flowable.create(new FlowableOnSubscribe<DownloadStatus>() {
                @Override
                public void subscribe(FlowableEmitter<DownloadStatus> e) throws Exception {
                    mDownloadHelper.saveNormalFile(e, mUrl, response);
                }
            }, BackpressureStrategy.BUFFER);
        }

        @Override
        public Observable<DownloadStatus> startDownload() {
            Log.i(FileHelper.TAG, "Normal download start!!");
            return mDownloadHelper.getDownloadApi().download(null, mUrl)
                    .subscribeOn(Schedulers.io())
                    .flatMap(new Function<Response<ResponseBody>, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(Response<ResponseBody> response) throws Exception {
                            return normalSave(response);
                        }
                    })
                    .retry(new BiPredicate<Integer, Throwable>() {
                        @Override
                        public boolean test(Integer integer, Throwable throwable) throws Exception {
                            return mDownloadHelper.retry(integer, throwable);
                        }
                    })
                    .toObservable();
        }
    }

    static class ContinueDownload extends DownloadType {

        /**
         * 分段下载的任务
         *
         * @param i 下载编号
         * @return Observable
         */
        private Flowable<DownloadStatus> rangeDownloadTask(final int i) {
            return Flowable.create(new FlowableOnSubscribe<DownloadRange>() {
                @Override
                public void subscribe(FlowableEmitter<DownloadRange> emitter) throws Exception {
                    DownloadRange range = mDownloadHelper.readDownloadRange(mUrl, i);
                    if (range.start <= range.end) {
                        emitter.onNext(range);
                    }
                    emitter.onComplete();
                }
            }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io()).flatMap(new Function<DownloadRange, Publisher<DownloadStatus>>() {
                @Override
                public Publisher<DownloadStatus> apply(final DownloadRange downloadRange) throws Exception {
                    String range = "bytes=" + downloadRange.start + "-" + downloadRange.end;
                    return mDownloadHelper.getDownloadApi().download(range, mUrl)
                            .flatMap(new Function<Response<ResponseBody>, Publisher<DownloadStatus>>() {
                                @Override
                                public Publisher<DownloadStatus> apply(Response<ResponseBody> response) throws Exception {
                                    return rangeSave(downloadRange.start, downloadRange.end, i, response.body());
                                }
                            });
                }
            }).retry(new BiPredicate<Integer, Throwable>() {
                @Override
                public boolean test(Integer integer, Throwable throwable) throws Exception {
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
        private Flowable<DownloadStatus> rangeSave(final long start, final long end, final int i,
                                                   final ResponseBody response) {
            return Flowable.create(new FlowableOnSubscribe<DownloadStatus>() {
                @Override
                public void subscribe(FlowableEmitter<DownloadStatus> emitter) throws Exception {
                    mDownloadHelper.saveRangeFile(emitter, i, start, end, mUrl, response);
                }
            }, BackpressureStrategy.LATEST);
        }

        @Override
        public void prepareDownload() throws IOException, ParseException {
            Log.i(FileHelper.TAG, "Continue download start!!");
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            List<Flowable<DownloadStatus>> tasks = new ArrayList<>();
            for (int i = 0; i < mDownloadHelper.getMaxThreads(); i++) {
                tasks.add(rangeDownloadTask(i));
            }
            return Flowable.mergeDelayError(tasks).toObservable();
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
                    .flatMap(new Function<DownloadType, ObservableSource<DownloadStatus>>() {
                        @Override
                        public ObservableSource<DownloadStatus> apply(DownloadType downloadType) throws Exception {
                            try {
                                downloadType.prepareDownload();
                                return downloadType.startDownload();
                            } catch (IOException | ParseException e) {
                                return Observable.error(e);
                            }
                        }
                    });
        }
    }
}
