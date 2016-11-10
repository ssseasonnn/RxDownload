package zlc.season.rxdownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/10
 * Time: 11:28
 * FIXME
 */
public class DownloadReceiver extends BroadcastReceiver {

    public static final String RX_BROADCAST_DOWNLOAD_START = "zlc.season.rxdownload.broadcast.intent.action.start";
    public static final String RX_BROADCAST_DOWNLOAD_NEXT = "zlc.season.rxdownload.broadcast.intent.action.next";
    public static final String RX_BROADCAST_DOWNLOAD_COMPLETE = "zlc.season.rxdownload.broadcast.intent.complete";
    public static final String RX_BROADCAST_DOWNLOAD_ERROR = "zlc.season.rxdownload.broadcast.intent.error";

    public static final String RX_BROADCAST_KEY_EXCEPTION = "zlc.season.rxdownload.broadcast.key.exception";
    public static final String RX_BROADCAST_KEY_STATUS = "zlc.season.rxdownload.broadcast.key.status";
    public static final String RX_BROADCAST_KEY_URL = "zlc.season.rxdownload.broadcast.key.url";

    private CallBack mCallBack;
    private String mKeyUrl;


    public DownloadReceiver(String keyUrl, CallBack callBack) {
        mKeyUrl = keyUrl;
        mCallBack = callBack;
    }

    public IntentFilter getFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RX_BROADCAST_DOWNLOAD_START);
        intentFilter.addAction(RX_BROADCAST_DOWNLOAD_NEXT);
        intentFilter.addAction(RX_BROADCAST_DOWNLOAD_COMPLETE);
        intentFilter.addAction(RX_BROADCAST_DOWNLOAD_ERROR);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String key = intent.getStringExtra(RX_BROADCAST_KEY_URL);
        switch (action) {
            case RX_BROADCAST_DOWNLOAD_START:
                if (key.compareTo(mKeyUrl) == 0) {
                    mCallBack.onDownloadStart();
                }
                break;
            case RX_BROADCAST_DOWNLOAD_NEXT:
                if (key.compareTo(mKeyUrl) == 0) {
                    mCallBack.onDownloadNext((DownloadStatus) intent.getParcelableExtra(RX_BROADCAST_KEY_STATUS));
                }
                break;
            case RX_BROADCAST_DOWNLOAD_COMPLETE:
                if (key.compareTo(mKeyUrl) == 0) {
                    mCallBack.onDownloadComplete();
                }
                break;
            case RX_BROADCAST_DOWNLOAD_ERROR:
                if (key.compareTo(mKeyUrl) == 0) {
                    Throwable e = (Throwable) intent.getSerializableExtra(RX_BROADCAST_KEY_EXCEPTION);
                    mCallBack.onDownloadError(e);
                }
                break;
        }
    }

    public interface CallBack {
        void onDownloadStart();

        void onDownloadNext(DownloadStatus status);

        void onDownloadComplete();

        void onDownloadError(Throwable e);
    }
}