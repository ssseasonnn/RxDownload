package zlc.season.rxdownloadproject;

import rx.Subscription;
import zlc.season.practicalrecyclerview.ItemType;

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

    String url;
    String image;
    int state;
    Subscription subscription;

    /**
     * 取消订阅,否则会内存泄漏
     */
    public void unsubscrbe() {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    @Override
    public int itemType() {
        return 0;
    }
}
