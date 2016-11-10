package zlc.season.rxdownload;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_DOWNLOAD_COMPLETE;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_DOWNLOAD_ERROR;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_DOWNLOAD_NEXT;
import static zlc.season.rxdownload.DownloadReceiver.RX_BROADCAST_DOWNLOAD_START;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/10
 * Time: 09:49
 * FIXME
 */
public class DownloadService extends Service {
    static final String RX_SERVICE_DOWNLOAD = "zlc.season.rxdownload.service.intent.action.serviceDownload";

    static final String RX_INTENT_SAVE_NAME = "zlc.season.rxdownload.service.intent.save_name";
    static final String RX_INTENT_SAVE_PATH = "zlc.season.rxdownload.service.intent.save_path";
    static final String RX_INTENT_DOWNLOAD_URL = "zlc.season.rxdownload.service.intent.download_url";

    private static final String TAG = "DownloadService";

    private RxDownload mRxDownload;

    private String mDownloadUrl;
    private String mSaveName;
    private String mSavePath;
    private DownloadBinder mBinder = new DownloadBinder();
    private CompositeSubscription mSubscriptions;


    public void setRxDownload(RxDownload rxDownload) {
        mRxDownload = rxDownload;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart");
        if (intent != null) {
            final String action = intent.getAction();
            if (RX_SERVICE_DOWNLOAD.equals(action)) {
                mDownloadUrl = intent.getStringExtra(RX_INTENT_DOWNLOAD_URL);
                mSaveName = intent.getStringExtra(RX_INTENT_SAVE_NAME);
                mSavePath = intent.getStringExtra(RX_INTENT_SAVE_PATH);
                startDownload(mDownloadUrl, mSaveName, mSavePath);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mSubscriptions.clear();
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

    private void startDownload(String url, String saveName, String savePath) {
        if (mRxDownload == null) {
            throw new NullPointerException("Some bad things happened! I can't download ...");
        }
        Subscription temp = mRxDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .sample(1, TimeUnit.SECONDS)
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_START);
                        sendBroadcast(intent);
                    }

                    @Override
                    public void onCompleted() {
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_COMPLETE);
                        sendBroadcast(intent);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_ERROR);
                        sendBroadcast(intent);
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        Intent intent = new Intent(RX_BROADCAST_DOWNLOAD_NEXT);
                        sendBroadcast(intent);
                    }
                });
        mSubscriptions.add(temp);
    }

    public class DownloadBinder extends Binder {

        DownloadService getService() {
            return DownloadService.this;
        }
    }
}
