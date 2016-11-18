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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import static zlc.season.rxdownload.DownloadFlag.CANCELED;
import static zlc.season.rxdownload.DownloadFlag.COMPLETED;
import static zlc.season.rxdownload.DownloadFlag.FAILED;
import static zlc.season.rxdownload.DownloadFlag.PAUSED;
import static zlc.season.rxdownload.DownloadFlag.STARTED;

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
    private Map<String, RecordValue> mDownloadRecord;
    private Map<String, Subject<DownloadStatus, DownloadStatus>> mSubjectPool;
    private Map<String, Subscription> mSubscriptionPool;
    private Queue<DownloadTask> mDownloadTaskQueue;

    private BehaviorSubject<String> mEventSubject = BehaviorSubject.create();
    private int MAX_DOWNLOAD_TASK = 5;
    private AtomicInteger currentDownloadTask = new AtomicInteger(0);

    private Thread mThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create Download Service...");
        mBinder = new DownloadBinder();
        mDownloadRecord = new HashMap<>();
        mSubjectPool = new HashMap<>();
        mSubscriptionPool = new HashMap<>();
        mDownloadTaskQueue = new LinkedList<>();
        mDb = new DataBaseHelper(new DbOpenHelper(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Start Download Service...");
        //TODO: read download record from database
        MAX_DOWNLOAD_TASK = intent.getIntExtra("MAX_DOWNLOAD_TASK", 5);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroy Download Service...");
        mThread.interrupt();
        for (RecordValue each : mDownloadRecord.values()) {
            Utils.unSubscribe(each.subscription);
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

    public void startDownload(RxDownload rxDownload, final Subject<DownloadStatus, DownloadStatus> subject,
                              final String url, String saveName, String savePath, String name, String image) {
        if (mDownloadRecord.get(url) != null) {
            Log.w(TAG, "This url download task already exists! So do nothing.");
            return;
        }

        final Subject<DownloadStatus, DownloadStatus> finalSubject;
        if (null == subject) {
            finalSubject = PublishSubject.create();
        } else {
            finalSubject = subject;
        }

        Subscription temp = rxDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .sample(500, TimeUnit.MILLISECONDS)
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        mDb.updateRecord(url, STARTED);
                    }

                    @Override
                    public void onCompleted() {
                        finalSubject.onCompleted();
                        Utils.unSubscribe(mDownloadRecord.get(url).subscription);
                        mDownloadRecord.remove(url);
                        mDb.updateRecord(url, COMPLETED);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        finalSubject.onError(e);
                        Utils.unSubscribe(mDownloadRecord.get(url).subscription);
                        mDownloadRecord.remove(url);
                        mDb.updateRecord(url, FAILED);
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        finalSubject.onNext(status);
                        mDb.updateRecord(url, status);
                    }
                });
        mDownloadRecord.put(url, new RecordValue(temp, finalSubject));
        if (mDb.recordNotExists(url)) {
            mDb.insertRecord(url, saveName, rxDownload.getFileSavePaths(savePath)[0], name, image);
        }
    }

    public void pauseDownload(String url) {
        if (null != mDownloadRecord.get(url)) {
            Utils.unSubscribe(mDownloadRecord.get(url).subscription);
            mDownloadRecord.remove(url);
        }
        mDb.updateRecord(url, PAUSED);
    }

    public void cancelDownload(String url) {
        if (null != mDownloadRecord.get(url)) {
            Utils.unSubscribe(mDownloadRecord.get(url).subscription);
            mDownloadRecord.remove(url);
        }
        mDb.updateRecord(url, CANCELED);
    }

    public void deleteDownload(String url) {
        if (null != mDownloadRecord.get(url)) {
            Utils.unSubscribe(mDownloadRecord.get(url).subscription);
            mDownloadRecord.remove(url);
        }
        mDb.deleteRecord(url);
    }

    static class RecordValue {
        Subscription subscription;
        Subject<DownloadStatus, DownloadStatus> subject;

        RecordValue(Subscription subscription, Subject<DownloadStatus, DownloadStatus> subject) {
            this.subscription = subscription;
            this.subject = subject;
        }
    }

    class DownloadTaskDispatchRunnable implements Runnable {

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
                    } else {
                        Log.d(TAG, "need wait");
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
