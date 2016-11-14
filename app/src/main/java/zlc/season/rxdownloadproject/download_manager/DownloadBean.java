package zlc.season.rxdownloadproject.download_manager;

import rx.Subscription;
import zlc.season.practicalrecyclerview.ItemType;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/28
 * Time: 09:30
 * FIXME
 */
public class DownloadBean implements ItemType {
    String name;
    String url;
    String image;
    int state;
    Subscription subscription;

    /**
     * 取消订阅
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
