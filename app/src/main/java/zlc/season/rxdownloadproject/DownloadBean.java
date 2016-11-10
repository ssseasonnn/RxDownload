package zlc.season.rxdownloadproject;

import android.content.Context;

import rx.Subscription;
import zlc.season.practicalrecyclerview.ItemType;
import zlc.season.rxdownload.DownloadReceiver;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/28
 * Time: 09:30
 * FIXME
 */
public class DownloadBean implements ItemType {
    public static final int START = 0;
    public static final int PAUSE = 1;
    public static final int DONE = 2;

    String name;
    String url;
    String image;
    int state;
    Subscription subscription;
    DownloadReceiver mReceiver;

    /**
     * 取消订阅
     */
    public void unsubscrbe() {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(mReceiver);
    }

    @Override
    public int itemType() {
        return 0;
    }
}
