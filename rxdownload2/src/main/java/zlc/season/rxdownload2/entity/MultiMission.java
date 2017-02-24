package zlc.season.rxdownload2.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.processors.FlowableProcessor;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;

import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_URL_EXISTS;
import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.paused;
import static zlc.season.rxdownload2.function.DownloadEventFactory.started;
import static zlc.season.rxdownload2.function.DownloadEventFactory.waiting;
import static zlc.season.rxdownload2.function.Utils.formatStr;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/24
 * <p>
 * MultiMission, many urls.
 */
public class MultiMission extends DownloadMission {
    private AtomicInteger completeNumber;
    private AtomicInteger failedNumber;
    private List<SingleMission> missions;

    private String missionId;

    private MultiMissionCallback callback = new MultiMissionCallback() {
        @Override
        public void start() {
            processor.onNext(started(null));
        }

        @Override
        public void next(DownloadStatus status) {

        }

        @Override
        public void error(Throwable throwable) {
            if ((failedNumber.incrementAndGet() + completeNumber.intValue()) == missions.size()) {
                processor.onNext(failed(null, new Throwable("download failedNumber")));
            }
        }

        @Override
        public void complete() {
            int temp = completeNumber.incrementAndGet();
            if (temp == missions.size()) {
                processor.onNext(completed(null));
            } else if ((temp + failedNumber.intValue()) == missions.size()) {
                processor.onNext(failed(null, new Throwable("download failedNumber")));
            }
        }
    };

    public MultiMission(RxDownload rxDownload, List<DownloadBean> missions, String missionId) {
        super(rxDownload);
        this.missionId = missionId;
        this.missions = new ArrayList<>();
        this.completeNumber = new AtomicInteger(0);
        this.failedNumber = new AtomicInteger(0);

        for (DownloadBean each : missions) {
            this.missions.add(new SingleMission(rxDownload, each, missionId, callback));
        }
    }

    public String getMissionId() {
        return missionId;
    }

    @Override
    public void init(Map<String, DownloadMission> missionMap,
                     Map<String, FlowableProcessor<DownloadEvent>> processorMap) {
        DownloadMission mission = missionMap.get(getMissionId());
        if (mission != null && !mission.isCancel()) {
            throw new IllegalArgumentException(formatStr(DOWNLOAD_URL_EXISTS, getMissionId()));
        } else {
            missionMap.put(getMissionId(), this);
        }

        processor = getProcessor(processorMap, getMissionId());
        for (SingleMission each : missions) {
            each.init(missionMap, processorMap);
        }
    }

    @Override
    public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.insertOrUpdate(dataBaseHelper);
        }
    }

    @Override
    public void sendWaitingEvent(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.sendWaitingEvent(dataBaseHelper);
        }
        processor.onNext(waiting(null));
    }

    @Override
    public void start(Semaphore semaphore) {
        for (SingleMission each : missions) {
            each.start(semaphore);
        }
    }

    @Override
    public void pause(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.pause(dataBaseHelper);
        }
        cancel();
        processor.onNext(paused(null));
    }

    @Override
    public void delete(DataBaseHelper dataBaseHelper, boolean deleteFile) {
        for (SingleMission each : missions) {
            each.delete(dataBaseHelper, deleteFile);
        }
    }
}
