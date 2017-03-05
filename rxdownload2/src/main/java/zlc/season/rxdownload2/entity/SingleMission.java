package zlc.season.rxdownload2.entity;

import java.util.Map;
import java.util.concurrent.Semaphore;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;
import zlc.season.rxdownload2.function.Constant;

import static zlc.season.rxdownload2.entity.DownloadFlag.WAITING;
import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.normal;
import static zlc.season.rxdownload2.function.DownloadEventFactory.paused;
import static zlc.season.rxdownload2.function.DownloadEventFactory.started;
import static zlc.season.rxdownload2.function.DownloadEventFactory.waiting;
import static zlc.season.rxdownload2.function.Utils.deleteFiles;
import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.formatStr;
import static zlc.season.rxdownload2.function.Utils.getFiles;
import static zlc.season.rxdownload2.function.Utils.log;

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
                     FlowableProcessor<DownloadEvent> processor) {
        DownloadMission mission = missionMap.get(getMissionId());
        if (mission == null) {
            missionMap.put(getMissionId(), this);
        } else {
            if (mission.isCanceled()) {
                missionMap.put(getMissionId(), this);
            } else {
                throw new IllegalArgumentException(formatStr(Constant.DOWNLOAD_URL_EXISTS, getMissionId()));
            }
        }
        this.processor = processor;
    }

    @Override
    public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
        if (dataBaseHelper.recordNotExists(getMissionId())) {
            dataBaseHelper.insertRecord(bean, WAITING);
        } else {
            dataBaseHelper.updateRecord(getMissionId(), WAITING);
        }
    }

    @Override
    public void sendWaitingEvent(DataBaseHelper dataBaseHelper) {
        processor.onNext(waiting(dataBaseHelper.readStatus(getMissionId())));
    }

    @Override
    public void start(final Semaphore semaphore) throws InterruptedException {
        if (isCanceled()) {
            return;
        }

        semaphore.acquire();

        if (isCanceled()) {
            semaphore.release();
            return;
        }

        disposable = rxdownload.download(bean)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {

                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        log("finally and release...");
                        semaphore.release();
                    }
                })
                .subscribe(new Consumer<DownloadStatus>() {
                    @Override
                    public void accept(DownloadStatus value) throws Exception {
                        status = value;
                        processor.onNext(started(value));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        processor.onNext(failed(status, throwable));
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        processor.onNext(completed(status));
                        setCompleted(true);
                    }
                });
    }

    @Override
    public void pause(DataBaseHelper dataBaseHelper) {
        dispose(disposable);
        setCanceled(true);
        processor.onNext(paused(dataBaseHelper.readStatus(getMissionId())));
    }

    @Override
    public void delete(DataBaseHelper dataBaseHelper, boolean deleteFile) {
        pause(dataBaseHelper);
        processor.onNext(normal(null));
        if (deleteFile) {
            DownloadRecord record = dataBaseHelper.readSingleRecord(getMissionId());
            if (record != null) {
                deleteFiles(getFiles(record.getSaveName(), record.getSavePath()));
            }
        }
        dataBaseHelper.deleteRecord(getMissionId());
    }
}