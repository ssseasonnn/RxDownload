package zlc.season.rxdownload2.entity;

import java.util.Map;
import java.util.concurrent.Semaphore;

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
 * SingleMission, only one url.
 */
public class SingleMission extends DownloadMission {
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
    public void init(Map<String, DownloadMission> missionMap,
                     Map<String, FlowableProcessor<DownloadEvent>> processorMap) {
        if (missionMap.containsKey(getMissionId())) {
            throw new IllegalArgumentException(formatStr(DOWNLOAD_URL_EXISTS, getMissionId()));
        }
        missionMap.put(getMissionId(), this);
        processor = getProcessor(processorMap, getMissionId());
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
    public void sendWaitingEvent(DataBaseHelper dataBaseHelper) {
        processor.onNext(waiting(dataBaseHelper.readStatus(getMissionId())));
    }

    @Override
    public void start(final Semaphore semaphore) {
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
    public void cancel() {
        super.cancel();
        dispose(disposable);
    }

    @Override
    public void delete(DataBaseHelper dataBaseHelper) {
        dataBaseHelper.deleteRecord(getMissionId());
    }
}