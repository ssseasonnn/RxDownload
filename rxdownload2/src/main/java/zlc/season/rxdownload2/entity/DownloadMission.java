package zlc.season.rxdownload2.entity;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;

import static zlc.season.rxdownload2.function.Constant.ACQUIRE_SUCCESS;
import static zlc.season.rxdownload2.function.Constant.TRY_TO_ACQUIRE_SEMAPHORE;
import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.log;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/18
 * Time: 11:38
 * <p>
 * Represents a download task
 */
public abstract class DownloadMission {
    protected RxDownload rxdownload;
    protected FlowableProcessor<DownloadEvent> processor;
    private AtomicBoolean canceled = new AtomicBoolean(false);

    public DownloadMission(RxDownload rxdownload) {
        this.rxdownload = rxdownload;
    }

    public void cancel() {
        canceled.compareAndSet(false, true);
    }

    public boolean isCancel() {
        return canceled.get();
    }

    protected FlowableProcessor<DownloadEvent> getProcessor(Map<String,
            FlowableProcessor<DownloadEvent>> processorMap, String missionId) {
        if (processorMap.get(missionId) == null) {
            FlowableProcessor<DownloadEvent> processor =
                    BehaviorProcessor.<DownloadEvent>create().toSerialized();
            processorMap.put(missionId, processor);
        }
        return processorMap.get(missionId);
    }

    public abstract String getMissionId();

    public abstract void init(Map<String, DownloadMission> missionMap,
                              Map<String, FlowableProcessor<DownloadEvent>> processorMap);

    public abstract void insertOrUpdate(DataBaseHelper dataBaseHelper);

    public abstract void start(final Semaphore semaphore);

    public abstract void pause(DataBaseHelper dataBaseHelper);

    public abstract void delete(DataBaseHelper dataBaseHelper, boolean deleteFile);

    public abstract void sendWaitingEvent(DataBaseHelper dataBaseHelper);

    protected Disposable start(DownloadBean bean, final Semaphore semaphore,
                               final MissionCallback callback) {
        return rxdownload.download(bean)
                .subscribeOn(Schedulers.io())
                .doOnLifecycle(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        if (canceled.get()) {
                            dispose(disposable);
                        }

                        log(TRY_TO_ACQUIRE_SEMAPHORE);
                        semaphore.acquire();
                        log(ACQUIRE_SUCCESS);

                        if (canceled.get()) {
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

    /**
     * Mission download callback.
     */
    interface MissionCallback {
        void start();

        void next(DownloadStatus status);

        void error(Throwable throwable);

        void complete();
    }

    interface MultiMissionCallback extends MissionCallback {

    }
}
