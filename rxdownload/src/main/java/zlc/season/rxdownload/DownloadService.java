package zlc.season.rxdownload;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_DOWNLOAD_COMPLETE;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_DOWNLOAD_ERROR;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_DOWNLOAD_NEXT;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_KEY_EXCEPTION;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_KEY_STATUS;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_KEY_URL;
import static zlc.season.rxdownload.DownloadRecord.FLAG_CANCELED;
import static zlc.season.rxdownload.DownloadRecord.FLAG_COMPLETED;
import static zlc.season.rxdownload.DownloadRecord.FLAG_FAILED;
import static zlc.season.rxdownload.DownloadRecord.FLAG_PAUSED;
import static zlc.season.rxdownload.DownloadRecord.FLAG_STARTED;

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
    private Map<String, Subscription> mRecordMap;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create Download Service...");
        mBinder = new DownloadBinder();
        mRecordMap = new HashMap<>();
        mDataBaseHelper = new DataBaseHelper(new DbOpenHelper(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Start Download Service...");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroy Download Service...");
        for (Subscription each : mRecordMap.values()) {
            Utils.unSubscribe(each);
        }
        mDataBaseHelper.closeDataBase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bind Download Service...");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbind Download Service...");
        return super.onUnbind(intent);
    }

    public void startDownload(RxDownload rxDownload, final String url, String saveName, String savePath,
                              String name, String image) {
        if (mRecordMap.get(url) != null) {
            Log.w(TAG, "This url download task already exists! So do nothing.");
            return;
        }
        Subscription temp = rxDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .sample(500, TimeUnit.MILLISECONDS)
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        mDataBaseHelper.updateRecord(url, FLAG_STARTED);
                    }

                    @Override
                    public void onCompleted() {
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_COMPLETE);
                        intent.putExtra(RX_BROADCAST_KEY_URL, url);
                        sendBroadcast(intent);
                        Utils.unSubscribe(mRecordMap.get(url));
                        mRecordMap.remove(url);
                        mDataBaseHelper.updateRecord(url, FLAG_COMPLETED);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_ERROR);
                        intent.putExtra(RX_BROADCAST_KEY_URL, url);
                        intent.putExtra(RX_BROADCAST_KEY_EXCEPTION, e);
                        sendBroadcast(intent);
                        Utils.unSubscribe(mRecordMap.get(url));
                        mRecordMap.remove(url);
                        mDataBaseHelper.updateRecord(url, FLAG_FAILED);
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_NEXT);
                        intent.putExtra(RX_BROADCAST_KEY_URL, url);
                        intent.putExtra(RX_BROADCAST_KEY_STATUS, status);
                        sendBroadcast(intent);
                        mDataBaseHelper.updateRecord(url, status);
                    }
                });
        mRecordMap.put(url, temp);
        if (mDataBaseHelper.recordNotExists(url)) {
            mDataBaseHelper.insertRecord(url, saveName, rxDownload.getFileSavePaths(savePath)[0], name, image);
        }
    }

    public void pauseDownload(String url) {
        Utils.unSubscribe(mRecordMap.get(url));
        mRecordMap.remove(url);
        mDataBaseHelper.updateRecord(url, FLAG_PAUSED);
    }

    public void cancelDownload(String url) {
        Utils.unSubscribe(mRecordMap.get(url));
        mRecordMap.remove(url);
        mDataBaseHelper.updateRecord(url, FLAG_CANCELED);
    }

    public void deleteDownload(String url) {
        Utils.unSubscribe(mRecordMap.get(url));
        mRecordMap.remove(url);
        mDataBaseHelper.deleteRecord(url);
    }

    public class DownloadBinder extends Binder {
        DownloadService getService() {
            return DownloadService.this;
        }
    }
}
