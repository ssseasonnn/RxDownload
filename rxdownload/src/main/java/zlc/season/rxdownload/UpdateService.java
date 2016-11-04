package zlc.season.rxdownload;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static zlc.season.rxdownload.UpdateService.DownloadReceiver.INTENT_ACTION_CANCEL;
import static zlc.season.rxdownload.UpdateService.DownloadReceiver.INTENT_ACTION_CONTINUE;
import static zlc.season.rxdownload.UpdateService.DownloadReceiver.INTENT_ACTION_PAUSE;
import static zlc.season.rxdownload.UpdateService.DownloadReceiver.INTENT_ACTION_RETRY;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016-08-11
 * Time: 11:24
 * 自动升级Service
 */
public class UpdateService extends Service {
    public static final String INTENT_SAVE_NAME = "zlc.season.rxdownload.service.intent.save_name";
    public static final String INTENT_DOWNLOAD_URL = "zlc.season.rxdownload.service.intent.download_url";

    private static final int NOTIFICATION_ID = UUID.randomUUID().hashCode();
    private static final String DOWNLOAD_SAVE_PATH =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

    private String mDownloadUrl;
    private String mSaveName;

    private NotificationManager mNotificationManager;
    private DownloadReceiver mDownloadReceiver;
    private NotificationCompat.Builder mBuilder;

    private Subscription mSubscription;

    private boolean first = true;

    private NotificationCompat.Action cancelAction;
    private NotificationCompat.Action pauseAction;
    private NotificationCompat.Action continueAction;
    private NotificationCompat.Action retryAction;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);

        registerReceiver();
        createActions();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mDownloadUrl = intent.getStringExtra(INTENT_DOWNLOAD_URL);
            mSaveName = intent.getStringExtra(INTENT_SAVE_NAME);
            startDownload();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mDownloadReceiver);
        cancelDownload();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createActions() {
        cancelAction = new NotificationCompat.Action(R.drawable.ic_cancel, getString(R.string.cancel_download),
                PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_CANCEL), FLAG_UPDATE_CURRENT));

        pauseAction = new NotificationCompat.Action(R.drawable.ic_pause, getString(R.string.pause_download),
                PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_PAUSE), FLAG_UPDATE_CURRENT));

        continueAction = new NotificationCompat.Action(R.drawable.ic_continue, getString(R.string.continue_download),
                PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_CONTINUE), FLAG_UPDATE_CURRENT));

        retryAction = new NotificationCompat.Action(R.drawable.ic_action_reload, getString(R.string.re_download),
                PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_RETRY), FLAG_UPDATE_CURRENT));
    }

    private void registerReceiver() {
        mDownloadReceiver = new DownloadReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_ACTION_CONTINUE);
        intentFilter.addAction(INTENT_ACTION_PAUSE);
        intentFilter.addAction(INTENT_ACTION_CANCEL);
        intentFilter.addAction(INTENT_ACTION_RETRY);
        registerReceiver(mDownloadReceiver, intentFilter);
    }

    /**
     * 取消下载
     */
    private void cancelDownload() {
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        first = true;
        mNotificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
    }

    private void onDownloadStart() {
        mBuilder.mActions.clear();
        mBuilder.setContentTitle(getString(R.string.title))
                .setContentText(getString(R.string.download_prepare))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(0, 0, true)
                .addAction(cancelAction);
        startForeground(NOTIFICATION_ID, mBuilder.build());
    }


    private void onDownloadNext(boolean isChunked, int progress, int max) {
        if (!isChunked && first) {
            mBuilder.mActions.clear();
            mBuilder.setContentText(getString(R.string.download_started))
                    .addAction(pauseAction)
                    .addAction(cancelAction);
        }
        first = false;
        mBuilder.setProgress(max, progress, isChunked);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void onDownloadFailed() {
        mBuilder.mActions.clear();
        mBuilder.setContentText(getString(R.string.download_failed))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setProgress(0, 0, false)
                .addAction(retryAction)
                .addAction(cancelAction);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void onDownloadComplete() {
        stopForeground(true);
        mBuilder.mActions.clear();
        mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentText(getString(R.string.download_completed))
                .setContentIntent(getDefaultIntent())
                .setProgress(0, 0, false);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private PendingIntent getDefaultIntent() {
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                    new File(DOWNLOAD_SAVE_PATH + File.separator + mSaveName));
        } else {
            uri = Uri.fromFile(new File(DOWNLOAD_SAVE_PATH + File.separator + mSaveName));
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        return PendingIntent.getActivity(this, 1, intent, FLAG_UPDATE_CURRENT);
    }


    private void pauseDownload() {
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        first = true;
        mBuilder.mActions.clear();
        mBuilder.setContentText(getString(R.string.download_paused))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setProgress(0, 0, false)
                .addAction(continueAction)
                .addAction(cancelAction);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void startDownload() {
        mSubscription = RxDownload.getInstance()
                .download(mDownloadUrl, mSaveName, DOWNLOAD_SAVE_PATH)
                .subscribeOn(Schedulers.io())
                .sample(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        onDownloadStart();
                    }

                    @Override
                    public void onCompleted() {
                        onDownloadComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        onDownloadFailed();
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        onDownloadNext(status.isChunked, (int) status.getDownloadSize(), (int) status.getTotalSize());
                    }
                });
    }

    public class DownloadReceiver extends BroadcastReceiver {

        public static final String INTENT_ACTION_PAUSE = "zlc.season.rxdownload.service.pauseDownload";
        public static final String INTENT_ACTION_CONTINUE = "zlc.season.rxdownload.service.continue";
        public static final String INTENT_ACTION_CANCEL = "zlc.season.rxdownload.service.cancel";
        public static final String INTENT_ACTION_RETRY = "zlc.season.rxdownload.service.retry";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case INTENT_ACTION_PAUSE:
                    pauseDownload();
                    break;
                case INTENT_ACTION_CONTINUE:
                    startDownload();
                    break;
                case INTENT_ACTION_CANCEL:
                    cancelDownload();
                    break;
                case INTENT_ACTION_RETRY:
                    startDownload();
                    break;
            }
        }
    }
}
