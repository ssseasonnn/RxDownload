package zlc.season.rxdownload2.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.disposables.Disposable;
import io.reactivex.processors.FlowableProcessor;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;

import static zlc.season.rxdownload2.entity.DownloadFlag.WAITING;
import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_URL_EXISTS;
import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.started;
import static zlc.season.rxdownload2.function.DownloadEventFactory.waiting;
import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.formatStr;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/24
 * <p>
 * MultiMission, can add many urls.
 */
public class MultiMission extends DownloadMission {
    private AtomicInteger completed;
    private AtomicInteger failed;
    private List<DownloadBean> missions;

    private Map<String, FlowableProcessor<DownloadEvent>> processorMap;
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

    public String getMissionId() {
        return missionId;
    }

    @Override
    public void init(Map<String, DownloadMission> missionMap,
                     Map<String, FlowableProcessor<DownloadEvent>> processorMap) {
        if (missionMap.containsKey(getMissionId())) {
            throw new IllegalArgumentException(formatStr(DOWNLOAD_URL_EXISTS, getMissionId()));
        }
        this.processorMap = processorMap;
        getProcessor(processorMap, getMissionId());

        for (DownloadBean each : missions) {
            if (missionMap.containsKey(each.getUrl())) {
                throw new IllegalArgumentException(formatStr(DOWNLOAD_URL_EXISTS, each.getUrl()));
            }
            missionMap.put(each.getUrl(), new SingleMission(rxdownload, each));
            getProcessor(processorMap, each.getUrl());
        }
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
    public void sendWaitingEvent(DataBaseHelper dataBaseHelper) {
        final FlowableProcessor<DownloadEvent> missionProcessor = processorMap.get(getMissionId());
        missionProcessor.onNext(waiting(null));

        for (final DownloadBean each : missions) {
            final FlowableProcessor<DownloadEvent> eachProcessor = processorMap.get(each.getUrl());
            eachProcessor.onNext(waiting(dataBaseHelper.readStatus(each.getUrl())));
        }
    }

    @Override
    public void start(Semaphore semaphore) {
        final FlowableProcessor<DownloadEvent> missionProcessor = processorMap.get(getMissionId());

        for (final DownloadBean each : missions) {
            final FlowableProcessor<DownloadEvent> eachProcessor = processorMap.get(each.getUrl());
            Disposable disposable = start(each, semaphore, new DownloadCallback() {
                @Override
                public void start() {
                    missionProcessor.onNext(started(null));
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
                        missionProcessor.onNext(failed(null, new Throwable("download failed")));
                    }
                }

                @Override
                public void complete() {
                    eachProcessor.onNext(completed(statusMap.get(each)));
                    int temp = completed.incrementAndGet();
                    if (temp == missions.size()) {
                        missionProcessor.onNext(completed(null));
                    } else if ((temp + failed.intValue()) == missions.size()) {
                        missionProcessor.onNext(failed(null, new Throwable("download failed")));
                    }
                }
            });
            disposableMap.put(each, disposable);
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
}
