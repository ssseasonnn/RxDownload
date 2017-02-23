package zlc.season.rxdownload2.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;

import static zlc.season.rxdownload2.function.Constant.ACQUIRE_SUCCESS;
import static zlc.season.rxdownload2.function.Constant.ACQUIRE_SURPLUS_SEMAPHORE;
import static zlc.season.rxdownload2.function.Constant.RELEASE_SURPLUS_SEMAPHORE;
import static zlc.season.rxdownload2.function.Constant.TRY_TO_ACQUIRE_SEMAPHORE;
import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.started;
import static zlc.season.rxdownload2.function.DownloadEventFactory.waiting;
import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.formatStr;
import static zlc.season.rxdownload2.function.Utils.log;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/18
 * Time: 11:38
 * FIXME
 */
public abstract class DownloadMission {
    protected RxDownload rxdownload;
    private boolean canceled = false;

    public DownloadMission(RxDownload rxdownload) {
        this.rxdownload = rxdownload;
    }

    public void cancel() {
        canceled = true;
    }

    public abstract String getKey();

    protected FlowableProcessor<DownloadEvent> getProcessor(Map<String, FlowableProcessor<DownloadEvent>> processorMap, String key) {
        if (processorMap.get(key) == null) {
            FlowableProcessor<DownloadEvent> processor =
                    BehaviorProcessor.<DownloadEvent>create().toSerialized();
            processorMap.put(key, processor);
        }
        return processorMap.get(key);
    }

    public abstract void insertOrUpdate(DataBaseHelper dataBaseHelper);

    public abstract void start(final Semaphore semaphore,
                               final Map<String, FlowableProcessor<DownloadEvent>> processorMap);

    public Disposable start(DownloadBean bean, final Semaphore semaphore, final DownloadCallback callback) {

        return rxdownload.download(bean)
                .subscribeOn(Schedulers.io())
                .doOnLifecycle(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        log(TRY_TO_ACQUIRE_SEMAPHORE);
                        semaphore.acquire();
                        log(ACQUIRE_SUCCESS);
                        log(formatStr(ACQUIRE_SURPLUS_SEMAPHORE, semaphore.availablePermits()));
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        log(formatStr(RELEASE_SURPLUS_SEMAPHORE, semaphore.availablePermits() + 1));
                        semaphore.release();
                    }
                })
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        if (canceled) {
                            dispose(disposable);
                        } else {
                            callback.start();
                        }
                    }
                })
                .subscribe(new Consumer<DownloadStatus>() {
                    @Override
                    public void accept(DownloadStatus value) throws Exception {
                        callback.next(value);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        callback.error(throwable);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        callback.complete();
                    }
                });
    }

    public abstract DownloadEvent createWaitingEvent(DataBaseHelper dataBaseHelper);

    interface DownloadCallback {
        void start();

        void next(DownloadStatus status);

        void error(Throwable throwable);

        void complete();
    }

    public static class SingleMission extends DownloadMission {
        protected DownloadStatus status;
        protected Disposable disposable;
        private DownloadBean bean;
        private FlowableProcessor<DownloadEvent> processor;

        public SingleMission(RxDownload rxdownload, DownloadBean bean) {
            super(rxdownload);
            this.bean = bean;
        }

        @Override
        public void cancel() {
            super.cancel();
            dispose(disposable);
        }

        @Override
        public String getKey() {
            return bean.getUrl();
        }

        @Override
        public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
            if (dataBaseHelper.recordNotExists(getKey())) {
                dataBaseHelper.insertRecord(bean, DownloadFlag.WAITING, null);
            } else {
                dataBaseHelper.updateFlag(getKey(), DownloadFlag.WAITING);
            }
        }

        @Override
        public void start(final Semaphore semaphore, final Map<String, FlowableProcessor<DownloadEvent>> processorMap) {
            processor = getProcessor(processorMap, getKey());
            disposable = start(bean, semaphore, new DownloadCallback() {
                @Override
                public void start() {

                }

                @Override
                public void next(DownloadStatus value) {
                    status = value;
                    processor.onNext(started(value));
                }

                @Override
                public void error(Throwable throwable) {
                    processor.onNext(failed(status, throwable));
                }

                @Override
                public void complete() {
                    processor.onNext(completed(status));
                }
            });
        }

        @Override
        public DownloadEvent createWaitingEvent(DataBaseHelper dataBaseHelper) {
            return waiting(dataBaseHelper.readStatus(getKey()));
        }
    }


    public static class MultiMission extends DownloadMission {
        private AtomicInteger completed;
        private AtomicInteger failed;
        private List<DownloadBean> missions;

        private Map<DownloadBean, Disposable> disposableMap;
        private Map<DownloadBean, DownloadStatus> statusMap;

        private String group;

        public MultiMission(RxDownload rxDownload, List<DownloadBean> missions, String group) {
            super(rxDownload);
            this.missions = missions;
            this.group = group;

            completed = new AtomicInteger(0);
            failed = new AtomicInteger(0);

            disposableMap = new HashMap<>(missions.size());
            statusMap = new HashMap<>(missions.size());
        }

        public String getKey() {
            return group;
        }

        @Override
        public void cancel() {
            super.cancel();
            for (Disposable each : disposableMap.values()) {
                dispose(each);
            }
        }

        @Override
        public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
            for (DownloadBean each : missions) {
                if (dataBaseHelper.recordNotExists(each.getUrl(), group)) {
                    dataBaseHelper.insertRecord(each, DownloadFlag.WAITING, group);
                } else {
                    dataBaseHelper.updateFlag(each.getUrl(), DownloadFlag.WAITING);
                }
            }
        }

        @Override
        public void start(Semaphore semaphore, final Map<String, FlowableProcessor<DownloadEvent>> processorMap) {

            final FlowableProcessor<DownloadEvent> groupProcessor = getProcessor(processorMap, getKey());

            for (final DownloadBean each : missions) {
                final FlowableProcessor<DownloadEvent> eachProcessor = getProcessor(processorMap, each.getUrl());
                Disposable disposable = start(each, semaphore, new DownloadCallback() {
                    @Override
                    public void start() {
                        groupProcessor.onNext(started(null));
                    }

                    @Override
                    public void next(DownloadStatus status) {
                        statusMap.put(each, status);
                        eachProcessor.onNext(started(status));
                    }

                    @Override
                    public void error(Throwable throwable) {
                        eachProcessor.onNext(failed(statusMap.get(each), throwable));
                        if (failed.incrementAndGet() == missions.size()) {
                            groupProcessor.onNext(failed(null, new Throwable("download failed")));
                        }
                    }

                    @Override
                    public void complete() {
                        eachProcessor.onNext(completed(statusMap.get(each)));
                        if (completed.incrementAndGet() == missions.size()) {
                            groupProcessor.onNext(completed(null));
                        }
                    }
                });
                disposableMap.put(each, disposable);
            }
        }

        @Override
        public DownloadEvent createWaitingEvent(DataBaseHelper dataBaseHelper) {
            return waiting(null);
        }
    }
}
