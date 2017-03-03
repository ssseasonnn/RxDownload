package zlc.season.rxdownload2.entity;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;

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

    /**
     * Mission download callback.
     */
    interface MultiMissionCallback {
        void start();

        void next(DownloadStatus status);

        void error(Throwable throwable);

        void complete();
    }
}
