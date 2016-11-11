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
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_DOWNLOAD_START;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_KEY_EXCEPTION;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_KEY_STATUS;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_KEY_URL;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/10
 * Time: 09:49
 * FIXME
 */
public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    private DownloadBinder mBinder;
    private Map<String, Subscription> mRecord;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mBinder = new DownloadBinder();
        mRecord = new HashMap<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnBind");
        return super.onUnbind(intent);
    }


    public Subscription getSubscription(String url) {
        return mRecord.get(url);
    }

    public void startDownload(RxDownload rxDownload, final String url, String saveName, String savePath) {
        Subscription temp = rxDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .sample(1, TimeUnit.SECONDS)
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_START);
                        intent.putExtra(RX_BROADCAST_KEY_URL, url);
                        sendBroadcast(intent);
                    }

                    @Override
                    public void onCompleted() {
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_COMPLETE);
                        intent.putExtra(RX_BROADCAST_KEY_URL, url);
                        sendBroadcast(intent);
                        Utils.unSubscribe(mRecord.get(url));
                        mRecord.remove(url);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_ERROR);
                        intent.putExtra(RX_BROADCAST_KEY_URL, url);
                        intent.putExtra(RX_BROADCAST_KEY_EXCEPTION, e);
                        sendBroadcast(intent);
                        Utils.unSubscribe(mRecord.get(url));
                        mRecord.remove(url);
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_NEXT);
                        intent.putExtra(RX_BROADCAST_KEY_URL, url);
                        intent.putExtra(RX_BROADCAST_KEY_STATUS, status);
                        sendBroadcast(intent);
                    }
                });
        mRecord.put(url, temp);
    }

    public class DownloadBinder extends Binder {
        DownloadService getService() {
            return DownloadService.this;
        }
    }
}
