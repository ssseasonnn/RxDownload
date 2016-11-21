package zlc.season.rxdownload.util;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscription;
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
    private static final String TAG = "DownloadService";
    private DownloadBinder mBinder;
    private DataBaseHelper mDataBaseHelper;

    private Map<String, Subject<DownloadStatus, DownloadStatus>> mSubjectPool;
    private Map<String, Subscription> mSubscriptionPool;
    private Queue<DownloadMission> mWaitingForDownload;

    private int MAX_DOWNLOAD_TASK = 3;
    private volatile AtomicInteger mCount = new AtomicInteger(0);

    private Thread mDownloadQueueThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create Download Service...");
        mBinder = new DownloadBinder();

        mSubjectPool = new ConcurrentHashMap<>();
        mSubscriptionPool = new ConcurrentHashMap<>();
        mWaitingForDownload = new LinkedList<>();

        mDataBaseHelper = DataBaseHelper.getSingleton(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Start Download Service...");
        //TODO: read download record from database
        if (intent != null) {
            MAX_DOWNLOAD_TASK = intent.getIntExtra("MAX_DOWNLOAD_TASK", 3);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroy Download Service...");
        mDownloadQueueThread.interrupt();
        for (String each : mSubscriptionPool.keySet()) {
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
        mWaitingForDownload.offer(mission);
        mDataBaseHelper.insertRecord(mission);
    }

    public void pauseDownload(String url) {
        if (mSubscriptionPool.get(url) != null) {
            mCount.decrementAndGet();
        }
        Utils.unSubscribe(mSubscriptionPool.get(url));
        mDataBaseHelper.updateRecord(url, PAUSED);
        mSubscriptionPool.remove(url);
    }

    public void cancelDownload(String url) {
        if (mSubscriptionPool.get(url) != null) {
            mCount.decrementAndGet();
        }
        Utils.unSubscribe(mSubscriptionPool.get(url));
        mDataBaseHelper.updateRecord(url, CANCELED);
        mSubscriptionPool.remove(url);
    }

    public void deleteDownload(String url) {
        if (mSubscriptionPool.get(url) != null) {
            mCount.decrementAndGet();
        }
        Utils.unSubscribe(mSubscriptionPool.get(url));
        mDataBaseHelper.deleteRecord(url);
        mSubscriptionPool.remove(url);
    }


    private class DownloadTaskDispatchRunnable implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                DownloadMission mission = mWaitingForDownload.peek();
                if (null != mission) {
                    String url = mission.getUrl();
                    if (mSubscriptionPool.get(url) != null) {
                        mWaitingForDownload.remove();
                        continue;
                    }
                    if (mCount.get() < MAX_DOWNLOAD_TASK) {
                        mission.init(mSubjectPool, mSubscriptionPool, mCount, mDataBaseHelper);
                        mission.start();
                        mWaitingForDownload.remove();
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
