package zlc.season.rxdownload.function;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import zlc.season.rxdownload.db.DataBaseHelper;
import zlc.season.rxdownload.entity.DownloadMission;
import zlc.season.rxdownload.entity.DownloadStatus;

import static zlc.season.rxdownload.entity.DownloadFlag.CANCELED;
import static zlc.season.rxdownload.entity.DownloadFlag.PAUSED;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/10
 * Time: 09:49
 * FIXME
 */
public class DownloadService extends Service {
    public static final String INTENT_KEY = "zlc_season_rxdownload_max_download_number";
    private static final String TAG = "DownloadService";
    private DownloadBinder mBinder;
    private DataBaseHelper mDataBaseHelper;

    private Map<String, Subject<DownloadStatus, DownloadStatus>> mSubjectPool;

    private Map<String, DownloadMission> mNowDownloading;
    private Queue<DownloadMission> mWaitingForDownload;
    private Map<String, DownloadMission> mWaitingForDownloadLookUpMap;

    private int MAX_DOWNLOAD_NUMBER = 5;
    private volatile AtomicInteger mCount = new AtomicInteger(0);

    private Thread mDownloadQueueThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create Download Service...");
        mBinder = new DownloadBinder();

        mSubjectPool = new ConcurrentHashMap<>();
        mWaitingForDownload = new LinkedList<>();
        mWaitingForDownloadLookUpMap = new HashMap<>();
        mNowDownloading = new HashMap<>();

        mDataBaseHelper = DataBaseHelper.getSingleton(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Start Download Service...");
        //TODO: read download record from database
        if (intent != null) {
            MAX_DOWNLOAD_NUMBER = intent.getIntExtra(INTENT_KEY, 5);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroy Download Service...");
        mDownloadQueueThread.interrupt();
        for (String each : mNowDownloading.keySet()) {
            pauseDownload(each);
        }
        mDataBaseHelper.closeDataBase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bind Download Service...");
        mDownloadQueueThread = new Thread(new DownloadTaskDispatchRunnable());
        mDownloadQueueThread.start();
        return mBinder;
    }

    public Subject<DownloadStatus, DownloadStatus> getSubject(String url) {
        if (mSubjectPool.get(url) == null) {
            Subject<DownloadStatus, DownloadStatus> subject = PublishSubject.create();
            mSubjectPool.put(url, subject);
        }
        return mSubjectPool.get(url);
    }

    public void addDownloadMission(DownloadMission mission) {
        if (mDataBaseHelper.recordNotExists(mission.getUrl())) {
            mDataBaseHelper.insertRecord(mission);
        }
        mWaitingForDownload.offer(mission);
        mWaitingForDownloadLookUpMap.put(mission.getUrl(), mission);
    }

    public void pauseDownload(String url) {
        mDataBaseHelper.updateRecord(url, PAUSED);
        decreaseCountAndBook(url);
    }

    public void cancelDownload(String url) {
        mDataBaseHelper.updateRecord(url, CANCELED);
        decreaseCountAndBook(url);
    }

    public void deleteDownload(String url) {
        mDataBaseHelper.deleteRecord(url);
        decreaseCountAndBook(url);
    }

    private void decreaseCountAndBook(String url) {
        if (mNowDownloading.get(url) != null) {
            mCount.decrementAndGet();
            Utils.unSubscribe(mNowDownloading.get(url).getSubscription());
            mNowDownloading.remove(url);
        }
        if (mWaitingForDownloadLookUpMap.get(url) != null) {
            mWaitingForDownloadLookUpMap.get(url).canceled = true;
        }
    }

    private class DownloadTaskDispatchRunnable implements Runnable {

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
                        mission.start(mNowDownloading, getSubject(url), mCount, mDataBaseHelper);
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
