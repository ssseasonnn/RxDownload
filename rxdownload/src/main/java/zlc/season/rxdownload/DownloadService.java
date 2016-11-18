package zlc.season.rxdownload;

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
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import static zlc.season.rxdownload.DownloadFlag.CANCELED;
import static zlc.season.rxdownload.DownloadFlag.PAUSED;

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
    private Map<String, Subscription> mSubscriptionPool;
    private Queue<DownloadTask> mDownloadTaskQueue;

    private int MAX_DOWNLOAD_TASK = 3;
    private AtomicInteger currentDownloadTask = new AtomicInteger(0);

    private Thread mThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create Download Service...");
        mBinder = new DownloadBinder();
        mSubjectPool = new HashMap<>();
        mSubscriptionPool = new HashMap<>();
        mDownloadTaskQueue = new LinkedList<>();
        mDb = new DataBaseHelper(new DbOpenHelper(this));
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
        for (Subscription each : mSubscriptionPool.values()) {
            Utils.unSubscribe(each);
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

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbind Download Service...");
        return super.onUnbind(intent);
    }

    public Subject<DownloadStatus, DownloadStatus> getSubject(String url) {
        if (mSubjectPool.get(url) == null) {
            Subject<DownloadStatus, DownloadStatus> subject = PublishSubject.create();
            mSubjectPool.put(url, subject);
        }
        return mSubjectPool.get(url);
    }

    public void addDownloadTask(DownloadTask task) {
        mDownloadTaskQueue.offer(task);
    }

    public void pauseDownload(String url) {
        removeSubscription(url);
        mDb.updateRecord(url, PAUSED);
    }

    public void cancelDownload(String url) {
        removeSubscription(url);
        mDb.updateRecord(url, CANCELED);
    }

    public void deleteDownload(String url) {
        removeSubscription(url);
        mDb.deleteRecord(url);
    }

    private void removeSubscription(String url) {
        Utils.unSubscribe(mSubscriptionPool.get(url));
        mSubscriptionPool.remove(url);
    }

    private class DownloadTaskDispatchRunnable implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "Thread running");
            while (!Thread.currentThread().isInterrupted()) {
                DownloadTask task = mDownloadTaskQueue.peek();
                if (null != task) {
                    if (currentDownloadTask.get() < MAX_DOWNLOAD_TASK) {
                        Log.d(TAG, "can download");
                        task.start(mDb, currentDownloadTask, mSubjectPool, mSubscriptionPool);
                        mDownloadTaskQueue.remove();
                    }
                }
            }
        }
    }

    public class DownloadBinder extends Binder {
        DownloadService getService() {
            return DownloadService.this;
        }
    }
}
