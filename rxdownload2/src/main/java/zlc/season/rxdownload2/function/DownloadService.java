package zlc.season.rxdownload2.function;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadEventFactory;
import zlc.season.rxdownload2.entity.DownloadMission;
import zlc.season.rxdownload2.entity.DownloadRecord;

import static zlc.season.rxdownload2.entity.DownloadFlag.CANCELED;
import static zlc.season.rxdownload2.entity.DownloadFlag.DELETED;
import static zlc.season.rxdownload2.entity.DownloadFlag.PAUSED;
import static zlc.season.rxdownload2.entity.DownloadFlag.WAITING;
import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_URL_EXISTS;
import static zlc.season.rxdownload2.function.Utils.dispose;
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
    private DataBaseHelper mDb;
    private DownloadEventFactory mEventFactory;

    private volatile Map<String, FlowableProcessor<DownloadEvent>> mProcessorPool;
    private volatile AtomicInteger mCount = new AtomicInteger(0);

    private Map<String, DownloadMission> mNowDownloading;
    private Queue<DownloadMission> mWaitingForDownload;
    private Map<String, DownloadMission> mWaitingForDownloadLookUpMap;

    private int MAX_DOWNLOAD_NUMBER = 5;
    private Thread mDownloadQueueThread;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new DownloadBinder();
        mProcessorPool = new ConcurrentHashMap<>();
        mWaitingForDownload = new LinkedList<>();
        mWaitingForDownloadLookUpMap = new HashMap<>();
        mNowDownloading = new HashMap<>();
        mDb = DataBaseHelper.getSingleton(getApplicationContext());
        mEventFactory = DownloadEventFactory.getSingleton();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mDb.repairErrorFlag();
        if (intent != null) {
            MAX_DOWNLOAD_NUMBER = intent.getIntExtra(INTENT_KEY, 5);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDownloadQueueThread.interrupt();
        for (String each : mNowDownloading.keySet()) {
            pauseDownload(each);
        }
        mDb.closeDataBase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mDownloadQueueThread = new Thread(new DownloadMissionDispatchRunnable());
        mDownloadQueueThread.start();
        return mBinder;
    }

    public FlowableProcessor<DownloadEvent> processor(RxDownload rxDownload, String url) {
        FlowableProcessor<DownloadEvent> processor = processor(url);
        if (mDb.recordExists(url)) {
            DownloadRecord record = mDb.readSingleRecord(url);
            File file = rxDownload.getRealFiles(record.getSaveName(), record.getSavePath())[0];
            if (file.exists()) {
                processor.onNext(mEventFactory.create(url, record.getFlag(), record.getStatus()));
            } else {
                processor.onNext(mEventFactory.normal(url));
            }
        } else {
            processor.onNext(mEventFactory.normal(url));
        }
        return processor;
    }

    public FlowableProcessor<DownloadEvent> processor(String url) {
        if (mProcessorPool.get(url) == null) {
            FlowableProcessor<DownloadEvent> processor = BehaviorProcessor.<DownloadEvent>create()
                    .toSerialized();
            mProcessorPool.put(url, processor);
        }
        return mProcessorPool.get(url);
    }

    public void addDownloadMission(DownloadMission mission) {
        String url = mission.getUrl();
        if (mWaitingForDownloadLookUpMap.get(url) != null || mNowDownloading.get(url) != null) {
            log(DOWNLOAD_URL_EXISTS);
        } else {
            if (mDb.recordNotExists(url)) {
                mDb.insertRecord(mission);
                processor(url).onNext(mEventFactory.waiting(url));
            } else {
                mDb.updateRecord(url, WAITING);
                processor(url).onNext(mEventFactory.waiting(url, mDb.readStatus(url)));
            }
            mWaitingForDownload.offer(mission);
            mWaitingForDownloadLookUpMap.put(url, mission);
        }
    }

    public void pauseDownload(String url) {
        suspendAndSend(url, PAUSED);
        mDb.updateRecord(url, PAUSED);
    }

    public void cancelDownload(String url) {
        suspendAndSend(url, CANCELED);
        mDb.updateRecord(url, CANCELED);
    }

    public void deleteDownload(String url, boolean deleteFile, RxDownload rxDownload) {
        suspendAndSend(url, DELETED);
        if (deleteFile) {
            DownloadRecord record = mDb.readSingleRecord(url);
            File[] files = rxDownload.getRealFiles(record.getSaveName(), record.getSavePath());
            Utils.deleteFile(files);
        }
        mDb.deleteRecord(url);
    }

    private void suspendAndSend(String url, int flag) {
        if (mWaitingForDownloadLookUpMap.get(url) != null) {
            mWaitingForDownloadLookUpMap.get(url).canceled = true;
        }
        if (mNowDownloading.get(url) != null) {
            dispose(mNowDownloading.get(url).getDisposable());

            processor(url).onNext(mEventFactory.create(url, flag,
                    mNowDownloading.get(url).getStatus()));

            mCount.decrementAndGet();
            mNowDownloading.remove(url);
        } else {
            processor(url).onNext(mEventFactory.create(url, flag, mDb.readStatus(url)));
        }
        /**
         * send normal event when deleted!
         */
        if (flag == DELETED) {
            processor(url).onNext(mEventFactory.normal(url));
        }
    }

    private class DownloadMissionDispatchRunnable implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                DownloadMission mission = mWaitingForDownload.peek();
                if (null != mission) {
                    String url = mission.getUrl();
                    if (mission.canceled) {
                        mWaitingForDownload.remove();
                        mWaitingForDownloadLookUpMap.remove(url);
                        continue;
                    }
                    if (mNowDownloading.get(url) != null) {
                        mWaitingForDownload.remove();
                        mWaitingForDownloadLookUpMap.remove(url);
                        continue;
                    }
                    if (mCount.get() < MAX_DOWNLOAD_NUMBER) {
                        mission.start(mNowDownloading, mCount, mDb, mProcessorPool);
                        mWaitingForDownload.remove();
                        mWaitingForDownloadLookUpMap.remove(url);
                    }
                }
            }
        }
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }
}
