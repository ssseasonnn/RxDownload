package zlc.season.rxdownload2.function;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

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
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadMission;
import zlc.season.rxdownload2.entity.DownloadRecord;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/10
 * Time: 09:49
 * FIXME
 */
public class DownloadService extends Service {
    public static final String INTENT_KEY = "zlc_season_rxdownload_max_download_number";
    private DownloadBinder mBinder;
    private DataBaseHelper mDataBaseHelper;
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

        mDataBaseHelper = DataBaseHelper.getSingleton(this);
        mEventFactory = DownloadEventFactory.getSingleton();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mDataBaseHelper.repairErrorFlag();
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
        mDataBaseHelper.closeDataBase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mDownloadQueueThread = new Thread(new DownloadMissionDispatchRunnable());
        mDownloadQueueThread.start();
        return mBinder;
    }

    public FlowableProcessor<DownloadEvent> getProcessor(RxDownload rxDownload, String url) {
        FlowableProcessor<DownloadEvent> processor = createAndGet(url);
        if (!mDataBaseHelper.recordNotExists(url)) {
            DownloadRecord record = mDataBaseHelper.readSingleRecord(url);
            File file = rxDownload.getRealFiles(record.getSaveName(), record.getSavePath())[0];
            if (file.exists()) {
                processor.onNext(mEventFactory.factory(url, record.getFlag(), record.getStatus()));
            }
        }
        return processor;
    }

    public FlowableProcessor<DownloadEvent> createAndGet(String url) {
        if (mProcessorPool.get(url) == null) {
            FlowableProcessor<DownloadEvent> processor
                    = BehaviorProcessor
                    .createDefault(mEventFactory.factory(url, DownloadFlag.NORMAL, null))
                    .toSerialized();
            mProcessorPool.put(url, processor);
        }
        return mProcessorPool.get(url);
    }

    public void addDownloadMission(DownloadMission mission) {
        String url = mission.getUrl();
        if (mWaitingForDownloadLookUpMap.get(url) != null || mNowDownloading.get(url) != null) {
            Log.d("DownloadService", "This download mission is exists.");
        } else {
            if (mDataBaseHelper.recordNotExists(url)) {
                mDataBaseHelper.insertRecord(mission);
                createAndGet(url).onNext(mEventFactory.factory(url, DownloadFlag.WAITING, null));
            } else {
                mDataBaseHelper.updateRecord(url, DownloadFlag.WAITING);
                createAndGet(url).onNext(mEventFactory.factory(url, DownloadFlag.WAITING,
                        mDataBaseHelper.readStatus(url)));
            }
            mWaitingForDownload.offer(mission);
            mWaitingForDownloadLookUpMap.put(url, mission);
        }
    }

    public void pauseDownload(String url) {
        suspendDownloadAndSendEvent(url, DownloadFlag.PAUSED);
        mDataBaseHelper.updateRecord(url, DownloadFlag.PAUSED);
    }

    public void cancelDownload(String url) {
        suspendDownloadAndSendEvent(url, DownloadFlag.CANCELED);
        mDataBaseHelper.updateRecord(url, DownloadFlag.CANCELED);
    }

    public void deleteDownload(String url) {
        suspendDownloadAndSendEvent(url, DownloadFlag.DELETED);
        mDataBaseHelper.deleteRecord(url);
    }

    private void suspendDownloadAndSendEvent(String url, int flag) {
        if (mWaitingForDownloadLookUpMap.get(url) != null) {
            mWaitingForDownloadLookUpMap.get(url).canceled = true;
        }
        if (mNowDownloading.get(url) != null) {
            Utils.dispose(mNowDownloading.get(url).getDisposable());
            createAndGet(url).onNext(mEventFactory.factory(url, flag, mNowDownloading.get(url).getStatus()));
            mCount.decrementAndGet();
            mNowDownloading.remove(url);
        } else {
            createAndGet(url).onNext(mEventFactory.factory(url, flag, mDataBaseHelper.readStatus(url)));
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
                        mission.start(mNowDownloading, mCount, mDataBaseHelper, mProcessorPool);
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
