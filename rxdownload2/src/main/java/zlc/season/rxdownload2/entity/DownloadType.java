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
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;
import zlc.season.rxdownload2.function.Utils;

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
import static zlc.season.rxdownload2.function.Constant.UNABLE_DOWNLOAD_HINT;
import static zlc.season.rxdownload2.function.Utils.log;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 09:44
 * 下载类型
 */
public abstract class DownloadType {
    protected TemporaryRecord record;

    public DownloadType(TemporaryRecord record) {
        this.record = record;
    }

    public abstract void prepareDownload()
            throws IOException, ParseException;

    public abstract Observable<DownloadStatus> startDownload() throws IOException;

    static class NormalDownload extends DownloadType {

        public NormalDownload(TemporaryRecord record) {
            super(record);
        }

        @Override
        public void prepareDownload() throws IOException, ParseException {
            log(NORMAL_DOWNLOAD_PREPARE);
            record.prepareNormalDownload();
        }

        @Override
        public Observable<DownloadStatus> startDownload() {
            return Flowable.just(1)
                    .doOnSubscribe(new Consumer<Subscription>() {
                        @Override
                        public void accept(Subscription subscription) throws Exception {
                            log(NORMAL_DOWNLOAD_STARTED);
                        }
                    })
                    .flatMap(new Function<Integer, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(Integer integer) throws Exception {
                            return start();
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
                    .toObservable();
        }

        private Publisher<DownloadStatus> start() {
            return record.download()
                    .flatMap(new Function<Response<ResponseBody>, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(Response<ResponseBody> response) throws Exception {
                            return save(response);
                        }
                    })
                    .compose(Utils.<DownloadStatus>retry2(record.getMaxRetryCount()));
        }

        private Publisher<DownloadStatus> save(final Response<ResponseBody> response) {
            return Flowable.create(new FlowableOnSubscribe<DownloadStatus>() {
                @Override
                public void subscribe(FlowableEmitter<DownloadStatus> e) throws Exception {
                    record.save(e, response);
                }
            }, BackpressureStrategy.LATEST);
        }
    }

    static class ContinueDownload extends DownloadType {

        public ContinueDownload(TemporaryRecord record) {
            super(record);
        }

        @Override
        public void prepareDownload() throws IOException, ParseException {
            log(prepareLog());
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            List<Publisher<DownloadStatus>> tasks = new ArrayList<>();
            for (int i = 0; i < record.getMaxThreads(); i++) {
                tasks.add(rangeDownloadTask(i));
            }
            return Flowable.mergeDelayError(tasks)
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
         * 分段下载任务
         *
         * @param index 下载编号
         * @return Observable
         */
        private Publisher<DownloadStatus> rangeDownloadTask(final int index) {
            return record.rangeDownload(index)
                    .subscribeOn(Schedulers.io())
                    .flatMap(new Function<Response<ResponseBody>, Publisher<DownloadStatus>>() {
                        @Override
                        public Publisher<DownloadStatus> apply(Response<ResponseBody> response) throws Exception {
                            return save(index, response.body());
                        }
                    })
                    .compose(Utils.<DownloadStatus>retry2(record.getMaxRetryCount()));

        }

        /**
         * 保存断点下载的文件,以及下载进度
         *
         * @param index    下载编号
         * @param response 响应值
         * @return Flowable
         */
        private Publisher<DownloadStatus> save(final int index, final ResponseBody response) {

            return Flowable.create(new FlowableOnSubscribe<DownloadStatus>() {
                @Override
                public void subscribe(FlowableEmitter<DownloadStatus> emitter) throws Exception {
                    record.save(emitter, index, response);
                }
            }, BackpressureStrategy.LATEST);
        }
    }

    static class MultiThreadDownload extends ContinueDownload {

        public MultiThreadDownload(TemporaryRecord record) {
            super(record);
        }

        @Override
        public void prepareDownload() throws IOException, ParseException {
            super.prepareDownload();
            record.prepareRangeDownload();
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

        public AlreadyDownloaded(TemporaryRecord record) {
            super(record);
        }

        @Override
        public void prepareDownload() throws IOException, ParseException {
            log(ALREADY_DOWNLOAD_HINT);
        }

        @Override
        public Observable<DownloadStatus> startDownload() throws IOException {
            return Observable.just(new DownloadStatus(record.getContentLength(), record.getContentLength()));
        }
    }

    static class UnableDownload extends DownloadType {

        public UnableDownload(TemporaryRecord record) {
            super(record);
        }

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
