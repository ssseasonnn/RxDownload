package zlc.season.rxdownload2.function;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
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
import zlc.season.rxdownload2.entity.DownloadMission;
import zlc.season.rxdownload2.entity.DownloadRecord;
import zlc.season.rxdownload2.entity.DownloadStatus;

import static zlc.season.rxdownload2.function.DownloadEventFactory.createEvent;
import static zlc.season.rxdownload2.function.DownloadEventFactory.normal;
import static zlc.season.rxdownload2.function.DownloadEventFactory.paused;
import static zlc.season.rxdownload2.function.DownloadEventFactory.waiting;
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
        dispose(disposable);
        for (String each : missionMap.keySet()) {
            pauseDownload(each);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        log("bind Download Service");
        startDispatch();
        return mBinder;
    }

    /**
     * receive download event.
     *
     * @param url url
     * @return DownloadEvent
     */
    public FlowableProcessor<DownloadEvent> receiveDownloadEvent(String url) {
        FlowableProcessor<DownloadEvent> processor = getProcessor(url);
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

    public FlowableProcessor<DownloadEvent> getProcessor(String url) {
        if (processorMap.get(url) == null) {
            FlowableProcessor<DownloadEvent> processor =
                    BehaviorProcessor.<DownloadEvent>create().toSerialized();
            processorMap.put(url, processor);
        }
        return processorMap.get(url);
    }

    /**
     * Add this mission into download queue.
     *
     * @param mission mission
     * @throws InterruptedException
     */
    public void addDownloadMission(DownloadMission mission) throws InterruptedException {
        missionMap.put(mission.getUrl(), mission);
        getProcessor(mission.getUrl()).onNext(waiting(dataBaseHelper.readStatus(mission.getUrl())));
        downloadQueue.put(mission);
    }

    /**
     * pause download
     *
     * @param url url
     */
    public void pauseDownload(String url) {
        DownloadMission mission = missionMap.get(url);
        DownloadStatus status;
        if (mission == null) {
            status = new DownloadStatus();
        } else {
            mission.markCanceled();
            dispose(mission.getDisposable());
            status = mission.getStatus();
        }
        getProcessor(url).onNext(paused(status));
    }

    /**
     * delete download
     *
     * @param url        url
     * @param deleteFile whether delete file
     */
    public void deleteDownload(String url, boolean deleteFile) {
        DownloadMission mission = missionMap.get(url);
        if (mission != null) {
            mission.markCanceled();
            dispose(mission.getDisposable());
        }
        getProcessor(url).onNext(normal(null));

        if (deleteFile) {
            DownloadRecord record = dataBaseHelper.readSingleRecord(url);
            if (record != null) {
                deleteFiles(getFiles(record.getSaveName(), record.getSavePath()));
            }
        }
        dataBaseHelper.deleteRecord(url);
    }

    /**
     * start dispatch download queue.
     */
    private void startDispatch() {
        disposable = Observable
                .create(new ObservableOnSubscribe<DownloadMission>() {
                    @Override
                    public void subscribe(ObservableEmitter<DownloadMission> emitter) throws Exception {
                        while (!emitter.isDisposed()) {
                            DownloadMission mission;
                            try {
                                mission = downloadQueue.take();
                            } catch (InterruptedException e) {
                                continue;
                            }
                            emitter.onNext(mission);
                        }
                    }
                }).subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<DownloadMission>() {
                    @Override
                    public void accept(DownloadMission mission) throws Exception {
                        mission.start(semaphore, processorMap.get(mission.getUrl()));
                    }
                });
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }
}
