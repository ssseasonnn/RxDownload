package zlc.season.rxdownload2.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.FlowableProcessor;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;
import zlc.season.rxdownload2.function.Constant;

import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.normal;
import static zlc.season.rxdownload2.function.DownloadEventFactory.paused;
import static zlc.season.rxdownload2.function.DownloadEventFactory.started;
import static zlc.season.rxdownload2.function.DownloadEventFactory.waiting;
import static zlc.season.rxdownload2.function.Utils.createProcessor;
import static zlc.season.rxdownload2.function.Utils.formatStr;
import static zlc.season.rxdownload2.function.Utils.log;

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
    private Observer<DownloadStatus> observer;

    public MultiMission(MultiMission other) {
        super(other.rxdownload);
        this.missionId = other.getUrl();
        this.missions = new ArrayList<>();
        this.completeNumber = new AtomicInteger(0);
        this.failedNumber = new AtomicInteger(0);

        this.observer = new SingleMissionObserver(this);

        for (SingleMission each : other.getMissions()) {
            this.missions.add(new SingleMission(each, observer));
        }
    }


    public MultiMission(RxDownload rxDownload, String missionId, List<DownloadBean> missions) {
        super(rxDownload);
        this.missionId = missionId;
        this.missions = new ArrayList<>();
        this.completeNumber = new AtomicInteger(0);
        this.failedNumber = new AtomicInteger(0);

        this.observer = new SingleMissionObserver(this);

        for (DownloadBean each : missions) {
            this.missions.add(new SingleMission(rxDownload, each, missionId, observer));
        }
    }

    public String getUrl() {
        return missionId;
    }

    @Override
    public void init(Map<String, DownloadMission> missionMap,
                     Map<String, FlowableProcessor<DownloadEvent>> processorMap) {
        DownloadMission mission = missionMap.get(getUrl());
        if (mission == null) {
            missionMap.put(getUrl(), this);
        } else {
            if (mission.isCanceled()) {
                missionMap.put(getUrl(), this);
            } else {
                throw new IllegalArgumentException(formatStr(Constant.DOWNLOAD_URL_EXISTS, getUrl()));
            }
        }

        this.processor = createProcessor(getUrl(), processorMap);

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
    public void start(Semaphore semaphore) throws InterruptedException {
        for (SingleMission each : missions) {
            each.start(semaphore);
        }
    }

    @Override
    public void pause(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.pause(dataBaseHelper);
        }
        setCanceled(true);
        completeNumber.set(0);
        failedNumber.set(0);
        processor.onNext(paused(null));
    }

    @Override
    public void delete(DataBaseHelper dataBaseHelper, boolean deleteFile) {
        for (SingleMission each : missions) {
            each.delete(dataBaseHelper, deleteFile);
        }
        setCanceled(true);
        completeNumber.set(0);
        failedNumber.set(0);
        processor.onNext(normal(null));
    }

    private List<SingleMission> getMissions() {
        return missions;
    }

    private class SingleMissionObserver implements Observer<DownloadStatus> {

        MultiMission real;

        public SingleMissionObserver(MultiMission multiMission) {
            this.real = multiMission;
        }

        @Override
        public void onSubscribe(Disposable d) {
            real.processor.onNext(started(null));
        }

        @Override
        public void onNext(DownloadStatus value) {

        }

        @Override
        public void onError(Throwable e) {
            log("onerror");
            int temp = real.failedNumber.incrementAndGet();
            log("temp: " + temp);
            log("size: " + real.missions.size());
            if ((temp + real.completeNumber.intValue()) == real.missions.size()) {
                real.processor.onNext(failed(null, new Throwable("download failed")));
                real.setCanceled(true);
                log("set error cancel");
            }
        }

        @Override
        public void onComplete() {
            int temp = real.completeNumber.incrementAndGet();
            if (temp == real.missions.size()) {
                real.processor.onNext(completed(null));
                real.setCompleted(true);
                real.setCanceled(true);
            } else if ((temp + real.failedNumber.intValue()) == real.missions.size()) {
                real.processor.onNext(failed(null, new Throwable("download failed")));
                real.setCanceled(true);
            }
        }
    }
}