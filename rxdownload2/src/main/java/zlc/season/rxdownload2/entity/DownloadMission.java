package zlc.season.rxdownload2.entity;

import java.util.Map;
import java.util.concurrent.Semaphore;

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
    FlowableProcessor<DownloadEvent> processor;
    private boolean canceled = false;
    private boolean completed = false;

    DownloadMission(RxDownload rxdownload) {
        this.rxdownload = rxdownload;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public abstract String getUrl();

    public abstract void init(Map<String, DownloadMission> missionMap,
                              Map<String, FlowableProcessor<DownloadEvent>> processorMap);

    public abstract void insertOrUpdate(DataBaseHelper dataBaseHelper);

    public abstract void start(final Semaphore semaphore) throws InterruptedException;

    public abstract void pause(DataBaseHelper dataBaseHelper);

    public abstract void delete(DataBaseHelper dataBaseHelper, boolean deleteFile);

    public abstract void sendWaitingEvent(DataBaseHelper dataBaseHelper);
}
