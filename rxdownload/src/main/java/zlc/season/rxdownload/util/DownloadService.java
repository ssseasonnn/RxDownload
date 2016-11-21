package zlc.season.rxdownload.util;

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
import zlc.season.rxdownload.BuildConfig;
import zlc.season.rxdownload.db.DataBaseHelper;
import zlc.season.rxdownload.entity.DownloadMission;
import zlc.season.rxdownload.entity.DownloadStatus;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/10
 * Time: 09:49
 * FIXME
 */
public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    private DownloadBinder mBinder;
    private DataBaseHelper mDb;
    private Map<String, Subject<DownloadStatus, DownloadStatus>> mSubjectPool;
    private Map<String, DownloadMission> mDownloadMissionPool;
    private Queue<DownloadMission> mWaitingForDownload;

    private int MAX_DOWNLOAD_TASK = 3;
    private AtomicInteger mCount = new AtomicInteger(0);

    private Thread mThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create Download Service...");
        mBinder = new DownloadBinder();

        mSubjectPool = new ConcurrentHashMap<>();
        mDownloadMissionPool = new HashMap<>();
        mWaitingForDownload = new LinkedList<>();

        mDb = DataBaseHelper.getSingleton(this);
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
        mThread.interrupt();
        for (DownloadMission mission : mDownloadMissionPool.values()) {
            mission.pause();
        }
        mDb.closeDataBase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bind Download Service...");
        mThread = new Thread(new DownloadTaskDispatchRunnable());
        mThread.start();
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
    }

    public void pauseDownload(String url) {
        DownloadMission mission = mDownloadMissionPool.get(url);
        if (mission != null) {
            mission.pause();
        }
        mDownloadMissionPool.remove(url);
    }

    public void cancelDownload(String url) {
        DownloadMission mission = mDownloadMissionPool.get(url);
        if (mission != null) {
            mission.cancel();
        }
        mDownloadMissionPool.remove(url);
    }

    public void deleteDownload(String url) {
        DownloadMission mission = mDownloadMissionPool.get(url);
        if (mission != null) {
            mission.delete();
        }
        mDownloadMissionPool.remove(url);
    }


    private class DownloadTaskDispatchRunnable implements Runnable {
        private int log = 0;

        @Override
        public void run() {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Download mission dispatch thread is running.");
            }
            while (!Thread.currentThread().isInterrupted()) {
                DownloadMission mission = mWaitingForDownload.peek();
                if (null != mission) {
                    final String url = mission.getUrl();
                    if (mDownloadMissionPool.get(url) != null) {
                        Log.w(TAG, "This url download task already exists! So do nothing.");
                        mWaitingForDownload.remove();
                        continue;
                    }
                    if (mDb.recordNotExists(url)) {
                        mDb.insertRecord(mission);
                    }
                    if (mCount.get() < MAX_DOWNLOAD_TASK) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Can download, so downloading.");
                            log = 0;
                        }
                        mission.init(mSubjectPool, mCount, mDb);
                        mission.start();
                        mDownloadMissionPool.put(url, mission);
                        mWaitingForDownload.remove();
                    } else {
                        if (BuildConfig.DEBUG && log == 0) {
                            Log.d(TAG, "Current download mission number is maximum, so wait.");
                            log++;
                        }
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
