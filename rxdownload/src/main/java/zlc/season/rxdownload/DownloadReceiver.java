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

    private CallBack mCallBack;

    public DownloadReceiver(CallBack callBack) {
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
        switch (action) {
            case RX_BROADCAST_DOWNLOAD_START:
                mCallBack.onDownloadStart();
                break;
            case RX_BROADCAST_DOWNLOAD_NEXT:
                mCallBack.onDownloadNext();
                break;
            case RX_BROADCAST_DOWNLOAD_COMPLETE:
                mCallBack.onDownloadComplete();
                break;
            case RX_BROADCAST_DOWNLOAD_ERROR:
                mCallBack.onDownloadError();
                break;
        }
    }

    public interface CallBack {
        void onDownloadStart();

        void onDownloadNext();

        void onDownloadComplete();

        void onDownloadError();
    }
}