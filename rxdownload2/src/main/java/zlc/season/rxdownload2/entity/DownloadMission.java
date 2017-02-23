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

import static zlc.season.rxdownload2.entity.DownloadFlag.WAITING;
import static zlc.season.rxdownload2.function.Constant.ACQUIRE_SUCCESS;
import static zlc.season.rxdownload2.function.Constant.TRY_TO_ACQUIRE_SEMAPHORE;
import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.started;
import static zlc.season.rxdownload2.function.DownloadEventFactory.waiting;
import static zlc.season.rxdownload2.function.Utils.dispose;
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

    public abstract String getMissionId();

    protected FlowableProcessor<DownloadEvent> getProcessor(Map<String,
            FlowableProcessor<DownloadEvent>> processorMap, String missionId) {
        if (processorMap.get(missionId) == null) {
            FlowableProcessor<DownloadEvent> processor =
                    BehaviorProcessor.<DownloadEvent>create().toSerialized();
            processorMap.put(missionId, processor);
        }
        return processorMap.get(missionId);
    }

    public abstract void insertOrUpdate(DataBaseHelper dataBaseHelper);

    public abstract void start(final Semaphore semaphore, final Map<String,
            FlowableProcessor<DownloadEvent>> processorMap);

    public abstract DownloadEvent createWaitingEvent(DataBaseHelper dataBaseHelper);

    public Disposable start(DownloadBean bean, final Semaphore semaphore,
                            final DownloadCallback callback) {

        return rxdownload.download(bean)
                .subscribeOn(Schedulers.io())
                .doOnLifecycle(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        log(TRY_TO_ACQUIRE_SEMAPHORE);
                        semaphore.acquire();
                        log(ACQUIRE_SUCCESS);
                        if (canceled) {
                            dispose(disposable);
                        } else {
                            callback.start();
                        }
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        semaphore.release();
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

    public abstract void delete(DataBaseHelper dataBaseHelper);

    /**
     * Mission download callback.
     */
    interface DownloadCallback {
        void start();

        void next(DownloadStatus status);

        void error(Throwable throwable);

        void complete();
    }

    /**
     * SingleMission, only one url.
     */
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
        public String getMissionId() {
            return bean.getUrl();
        }

        @Override
        public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
            if (dataBaseHelper.recordNotExists(getMissionId())) {
                dataBaseHelper.insertRecord(bean, WAITING, null);
            } else {
                dataBaseHelper.updateFlag(getMissionId(), WAITING);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            dispose(disposable);
        }

        @Override
        public void delete(DataBaseHelper dataBaseHelper) {
            dataBaseHelper.deleteRecord(getMissionId());
        }

        @Override
        public DownloadEvent createWaitingEvent(DataBaseHelper dataBaseHelper) {
            return waiting(dataBaseHelper.readStatus(getMissionId()));
        }

        @Override
        public void start(final Semaphore semaphore, final Map<String, FlowableProcessor<DownloadEvent>> processorMap) {
            processor = getProcessor(processorMap, getMissionId());
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
    }

    /**
     * MultiMission, maybe many urls.
     */
    public static class MultiMission extends DownloadMission {
        private AtomicInteger completed;
        private AtomicInteger failed;
        private List<DownloadBean> missions;

        private Map<DownloadBean, Disposable> disposableMap;
        private Map<DownloadBean, DownloadStatus> statusMap;

        private String missionId;

        public MultiMission(RxDownload rxDownload, List<DownloadBean> missions, String missionId) {
            super(rxDownload);
            this.missions = missions;
            this.missionId = missionId;

            completed = new AtomicInteger(0);
            failed = new AtomicInteger(0);

            disposableMap = new HashMap<>(missions.size());
            statusMap = new HashMap<>(missions.size());
        }

        @Override
        public DownloadEvent createWaitingEvent(DataBaseHelper dataBaseHelper) {
            return waiting(null);
        }

        public String getMissionId() {
            return missionId;
        }

        @Override
        public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
            for (DownloadBean each : missions) {
                if (dataBaseHelper.recordNotExists(each.getUrl())) {
                    dataBaseHelper.insertRecord(each, WAITING, missionId);
                } else {
                    dataBaseHelper.updateFlag(each.getUrl(), WAITING);
                }
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            for (Disposable each : disposableMap.values()) {
                dispose(each);
            }
        }

        @Override
        public void delete(DataBaseHelper dataBaseHelper) {
            for (DownloadBean each : missions) {
                dataBaseHelper.deleteRecord(each.getUrl());
            }
        }

        @Override
        public void start(Semaphore semaphore, final Map<String, FlowableProcessor<DownloadEvent>> processorMap) {

            final FlowableProcessor<DownloadEvent> groupProcessor = getProcessor(processorMap, getMissionId());

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
                        if ((failed.incrementAndGet() + completed.intValue()) == missions.size()) {
                            groupProcessor.onNext(failed(null, new Throwable("download failed")));
                        }
                    }

                    @Override
                    public void complete() {
                        eachProcessor.onNext(completed(statusMap.get(each)));
                        int temp = completed.incrementAndGet();
                        if (temp == missions.size()) {
                            groupProcessor.onNext(completed(null));
                        } else if ((temp + failed.intValue()) == missions.size()) {
                            groupProcessor.onNext(failed(null, new Throwable("download failed")));
                        }
                    }
                });
                disposableMap.put(each, disposable);
            }
        }
    }
}
