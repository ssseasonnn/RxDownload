package zlc.season.rxdownload2.function;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.db.DataBaseHelper;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadMission;
import zlc.season.rxdownload2.entity.DownloadRecord;

import static zlc.season.rxdownload2.function.Constant.WAITING_FOR_MISSION_COME;
import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.createEvent;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.normal;
import static zlc.season.rxdownload2.function.DownloadEventFactory.paused;
import static zlc.season.rxdownload2.function.Utils.deleteFiles;
import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.getFiles;
import static zlc.season.rxdownload2.function.Utils.log;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/10
 * Time: 09:49
 * FIXME
 */
public class DownloadService extends Service {
    public static final String INTENT_KEY = "zlc_season_rxdownload_max_download_number";

    private DownloadBinder mBinder;

    private Semaphore semaphore;
    private BlockingQueue<DownloadMission> downloadQueue;
    private Map<String, DownloadMission> missionMap;
    private Map<String, FlowableProcessor<DownloadEvent>> processorMap;

    private Disposable disposable;
    private DataBaseHelper dataBaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new DownloadBinder();
        downloadQueue = new LinkedBlockingQueue<>();
        processorMap = new ConcurrentHashMap<>();
        missionMap = new ConcurrentHashMap<>();

        dataBaseHelper = DataBaseHelper.getSingleton(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("start Download Service");
        dataBaseHelper.repairErrorFlag();
        if (intent != null) {
            int maxDownloadNumber = intent.getIntExtra(INTENT_KEY, 5);
            semaphore = new Semaphore(maxDownloadNumber);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("destroy Download Service");
        pauseAll();
        dataBaseHelper.closeDataBase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        log("bind Download Service");
        startDispatch();
        return mBinder;
    }

    private FlowableProcessor<DownloadEvent> createProcessor(String missionId) {
        if (processorMap.get(missionId) == null) {
            FlowableProcessor<DownloadEvent> processor =
                    BehaviorProcessor.<DownloadEvent>create().toSerialized();
            processorMap.put(missionId, processor);
        }
        return processorMap.get(missionId);
    }

    /**
     * receive download event for single url.
     *
     * @param url url
     * @return DownloadEvent
     */
    public FlowableProcessor<DownloadEvent> receiveDownloadEvent(String url) {
        FlowableProcessor<DownloadEvent> processor = createProcessor(url);
        DownloadMission mission = missionMap.get(url);
        if (mission == null) {  //Not yet add this url mission.
            DownloadRecord record = dataBaseHelper.readSingleRecord(url);
            if (record == null) {
                processor.onNext(normal(null));
            } else {
                File file = getFiles(record.getSaveName(), record.getSavePath())[0];
                if (file.exists()) {
                    processor.onNext(createEvent(record.getFlag(), record.getStatus()));
                } else {
                    processor.onNext(normal(null));
                }
            }
        }
        return processor;
    }

    /**
     * receive download event for multi mission.
     *
     * @param missionId missionId
     * @return DownloadEvent
     */
    public FlowableProcessor<DownloadEvent> receiveMissionsEvent(String missionId) {
        FlowableProcessor<DownloadEvent> processor = createProcessor(missionId);
        DownloadMission mission = missionMap.get(missionId);
        if (mission == null) {  //Not yet add this url mission.
            List<DownloadRecord> records = dataBaseHelper.readMissionsRecord(missionId);
            if (records.size() == 0) {
                processor.onNext(normal(null));
            } else {
                int failed = 0;
                for (DownloadRecord each : records) {
                    if (each.getFlag() == DownloadFlag.FAILED ||
                            !getFiles(each.getSaveName(), each.getSavePath())[0].exists()) {
                        failed++;
                        break;
                    }
                }
                if (failed > 0) {
                    processor.onNext(failed(null, new Throwable("download failed")));
                } else {
                    processor.onNext(completed(null));
                }
            }
        }
        return processor;
    }

    /**
     * Add this mission into download queue.
     *
     * @param mission mission
     * @throws InterruptedException
     */
    public void addDownloadMission(DownloadMission mission) throws InterruptedException {
        mission.init(missionMap, processorMap);
        mission.insertOrUpdate(dataBaseHelper);
        mission.sendWaitingEvent(dataBaseHelper);
        downloadQueue.put(mission);
    }

    /**
     * pause download
     *
     * @param missionId missionId
     */
    public void pauseDownload(String missionId) {
        cancelMission(missionId);
        createProcessor(missionId).onNext(paused(dataBaseHelper.readStatus(missionId)));
        missionMap.remove(missionId);
    }

    private void cancelMission(String missionId) {
        DownloadMission mission = missionMap.get(missionId);
        if (mission != null) {
            mission.cancel();
        }
    }

    /**
     * delete download
     *
     * @param missionId  missionId
     * @param deleteFile whether delete file
     */
    public void deleteDownload(String missionId, boolean deleteFile) {
        cancelMission(missionId);
        createProcessor(missionId).onNext(normal(null));
        if (deleteFile) {
            DownloadRecord record = dataBaseHelper.readSingleRecord(missionId);
            if (record != null) {
                deleteFiles(getFiles(record.getSaveName(), record.getSavePath()));
            }
        }
        deleteMission(missionId);
        missionMap.remove(missionId);
    }

    private void deleteMission(String missionId) {
        DownloadMission mission = missionMap.get(missionId);
        if (mission != null) {
            mission.delete(dataBaseHelper);
        }
    }

    /**
     * start dispatch download queue.
     */
    private void startDispatch() {
        disposable = Observable
                .create(new ObservableOnSubscribe<DownloadMission>() {
                    @Override
                    public void subscribe(ObservableEmitter<DownloadMission> emitter) throws Exception {
                        DownloadMission mission;
                        while (!emitter.isDisposed()) {
                            try {
                                log(WAITING_FOR_MISSION_COME);
                                mission = downloadQueue.take();
                                log(Constant.MISSION_COMING);
                            } catch (InterruptedException e) {
                                log("Interrupt blocking queue.");
                                continue;
                            }
                            emitter.onNext(mission);
                        }
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<DownloadMission>() {
                    @Override
                    public void accept(DownloadMission mission) throws Exception {
                        mission.start(semaphore);
                    }
                });
    }

    public void pauseAll() {
        dispose(disposable);
        for (String each : missionMap.keySet()) {
            pauseDownload(each);
        }
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }
}
