package zlc.season.rxdownload2.entity;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

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
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;
import zlc.season.rxdownload2.function.DownloadHelper;
import zlc.season.rxdownload2.function.Utils;

import static java.lang.Thread.currentThread;
import static zlc.season.rxdownload2.function.Constant.ALREADY_DOWNLOAD_HINT;
import static zlc.season.rxdownload2.function.Constant.CONTINUE_DOWNLOAD_COMPLETED;
import static zlc.season.rxdownload2.function.Constant.CONTINUE_DOWNLOAD_FAILED;
import static zlc.season.rxdownload2.function.Constant.CONTINUE_DOWNLOAD_PREPARE;
import static zlc.season.rxdownload2.function.Constant.CONTINUE_DOWNLOAD_STARTED;
import static zlc.season.rxdownload2.function.Constant.MULTITHREADING_DOWNLOAD_COMPLETED;
import static zlc.season.rxdownload2.function.Constant.MULTITHREADING_DOWNLOAD_FAILED;
import static zlc.season.rxdownload2.function.Constant.MULTITHREADING_DOWNLOAD_PREPARE;
import static zlc.season.rxdownload2.function.Constant.MULTITHREADING_DOWNLOAD_STARTED;
import static zlc.season.rxdownload2.function.Constant.NORMAL_DOWNLOAD_COMPLETED;
import static zlc.season.rxdownload2.function.Constant.NORMAL_DOWNLOAD_FAILED;
import static zlc.season.rxdownload2.function.Constant.NORMAL_DOWNLOAD_PREPARE;
import static zlc.season.rxdownload2.function.Constant.NORMAL_DOWNLOAD_STARTED;
import static zlc.season.rxdownload2.function.Constant.NOT_SUPPORT_HEAD_HINT;
import static zlc.season.rxdownload2.function.Constant.RANGE_DOWNLOAD_COMPLETED;
import static zlc.season.rxdownload2.function.Constant.RANGE_DOWNLOAD_STARTED;
import static zlc.season.rxdownload2.function.Constant.UNABLE_DOWNLOAD_HINT;
import static zlc.season.rxdownload2.function.Utils.log;

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

    public abstract void prepareDownload()
            throws IOException, ParseException;

    public abstract Observable<DownloadStatus> startDownload() throws IOException;

    static class NormalDownload extends DownloadType {

        @Override
        public void prepareDownload() throws IOException, ParseException {
            log(NORMAL_DOWNLOAD_PREPARE);
            mDownloadHelper.prepareNormalDownload(mUrl, mFileLength, mLastModify);
        }

        @Override
        public Observable<DownloadStatus> startDownload() {
            return mDownloadHelper
                    .getDownloadApi()
                    .download(null, mUrl)
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe(new Consumer<Subscription>() {
                        @Override
                        public void accept(Subscription subscription) throws Exception {
                            log(NORMAL_DOWNLOAD_STARTED);
                        }
                    })
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            log(NORMAL_DOWNLOAD_FAILED);
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            log(NORMAL_DOWNLOAD_COMPLETED);
                        }
                    })
                    .flatMap(new Function<Response<ResponseBody>, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(Response<ResponseBody> response)
                                throws Exception {
                            return normalSave(response);
                        }
                    })
                    .compose(Utils.<DownloadStatus>retry2(mDownloadHelper.getMaxRetryCount()))
                    .toObservable();
        }

        private Publisher<DownloadStatus> normalSave(final Response<ResponseBody> response) {
            return Flowable
                    .create(new FlowableOnSubscribe<DownloadStatus>() {
                        @Override
                        public void subscribe(FlowableEmitter<DownloadStatus> e)
                                throws Exception {
                            mDownloadHelper.saveNormalFile(e, mUrl, response);
                        }
                    }, BackpressureStrategy.LATEST);
        }
    }

    static class ContinueDownload extends DownloadType {

        @Override
        public void prepareDownload() throws IOException, ParseException {
            log(prepareLog());
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            List<Publisher<DownloadStatus>> tasks = new ArrayList<>();
            for (int i = 0; i < mDownloadHelper.getMaxThreads(); i++) {
                tasks.add(rangeDownloadTask(i));
            }
            return Flowable
                    .mergeDelayError(tasks)
                    .doOnSubscribe(new Consumer<Subscription>() {
                        @Override
                        public void accept(Subscription subscription) throws Exception {
                            log(startLog());
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            log(completeLog());
                        }
                    })
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            log(errorLog());
                        }
                    }).toObservable();
        }

        protected String prepareLog() {
            return CONTINUE_DOWNLOAD_PREPARE;
        }

        protected String startLog() {
            return CONTINUE_DOWNLOAD_STARTED;
        }

        protected String completeLog() {
            return CONTINUE_DOWNLOAD_COMPLETED;
        }

        protected String errorLog() {
            return CONTINUE_DOWNLOAD_FAILED;
        }

        /**
         * 分段下载的任务
         *
         * @param index 下载编号
         *
         * @return Observable
         */
        private Publisher<DownloadStatus> rangeDownloadTask(final int index) {

            return Flowable
                    .create(new FlowableOnSubscribe<DownloadRange>() {
                        @Override
                        public void subscribe(FlowableEmitter<DownloadRange> emitter)
                                throws Exception {
                            DownloadRange mRange = mDownloadHelper.readDownloadRange(mUrl, index);
                            if (mRange.legal()) {
                                emitter.onNext(mRange);
                            }
                            emitter.onComplete();
                        }
                    }, BackpressureStrategy.ERROR)
                    .subscribeOn(Schedulers.io())
                    .doOnNext(new Consumer<DownloadRange>() {
                        @Override
                        public void accept(DownloadRange range) throws Exception {
                            log(RANGE_DOWNLOAD_STARTED, currentThread().getName(),
                                    range.start, range.end);
                        }
                    })
                    .flatMap(new Function<DownloadRange, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(final DownloadRange range)
                                throws Exception {

                            return startRangeDownload(range, index);
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            log(RANGE_DOWNLOAD_COMPLETED, currentThread().getName());
                        }
                    })
                    .compose(Utils.<DownloadStatus>retry2(mDownloadHelper.getMaxRetryCount()));
        }

        /**
         * 开始下载
         *
         * @param range Range
         * @param index index
         *
         * @return DownloadStatus
         */
        private Publisher<DownloadStatus> startRangeDownload(final DownloadRange range,
                final int index) {

            String rangeStr = "bytes=" + range.start + "-" + range.end;
            return mDownloadHelper
                    .getDownloadApi()
                    .download(rangeStr, mUrl)
                    .flatMap(new Function<Response<ResponseBody>, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(Response<ResponseBody> resp)
                                throws Exception {
                            return rangeSave(range.start, range.end, index, resp.body());
                        }
                    });
        }

        /**
         * 保存断点下载的文件,以及下载进度
         *
         * @param start    从start开始
         * @param end      到end结束
         * @param index    下载编号
         * @param response 响应值
         *
         * @return Flowable
         */
        private Publisher<DownloadStatus> rangeSave(
                final long start, final long end,
                final int index,
                final ResponseBody response) {

            return Flowable
                    .create(new FlowableOnSubscribe<DownloadStatus>() {
                        @Override
                        public void subscribe(FlowableEmitter<DownloadStatus> emitter)
                                throws Exception {
                            mDownloadHelper.saveRangeFile(emitter,
                                    index, start, end, mUrl, response);
                        }
                    }, BackpressureStrategy.LATEST);
        }
    }

    static class MultiThreadDownload extends ContinueDownload {
        @Override
        public void prepareDownload() throws IOException, ParseException {
            super.prepareDownload();
            mDownloadHelper.prepareMultiThreadDownload(mUrl, mFileLength, mLastModify);
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            return super.startDownload();
        }

        @Override
        protected String prepareLog() {
            return MULTITHREADING_DOWNLOAD_PREPARE;
        }

        @Override
        protected String startLog() {
            return MULTITHREADING_DOWNLOAD_STARTED;
        }

        @Override
        protected String completeLog() {
            return MULTITHREADING_DOWNLOAD_COMPLETED;
        }

        @Override
        protected String errorLog() {
            return MULTITHREADING_DOWNLOAD_FAILED;
        }
    }

    static class AlreadyDownloaded extends DownloadType {

        @Override
        public void prepareDownload() throws IOException, ParseException {
            log(ALREADY_DOWNLOAD_HINT);
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            return Observable.just(new DownloadStatus(mFileLength, mFileLength));
        }
    }

    static class NotSupportHEAD extends DownloadType {

        @Override
        public void prepareDownload() throws IOException, ParseException {
            log(NOT_SUPPORT_HEAD_HINT);
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            return mDownloadHelper
                    .notSupportHead(mUrl)
                    .flatMap(new Function<DownloadType, ObservableSource<DownloadStatus>>() {
                        @Override
                        public ObservableSource<DownloadStatus> apply(DownloadType downloadType)
                                throws Exception {
                            downloadType.prepareDownload();
                            return downloadType.startDownload();
                        }
                    });
        }
    }

    static class UnableDownload extends DownloadType {

        @Override
        public void prepareDownload() throws IOException, ParseException {
            log(UNABLE_DOWNLOAD_HINT);
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            return Observable.error(new UnableDownloadException(UNABLE_DOWNLOAD_HINT));
        }
    }
}
