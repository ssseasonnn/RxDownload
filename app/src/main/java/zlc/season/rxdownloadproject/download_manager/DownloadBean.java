package zlc.season.rxdownloadproject.download_manager;

import rx.subscriptions.CompositeSubscription;
import zlc.season.practicalrecyclerview.ItemType;
import zlc.season.rxdownload.entity.DownloadRecord;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/28
 * Time: 09:30
 * FIXME
 */
public class DownloadBean implements ItemType {
    DownloadRecord mRecord;
    CompositeSubscription mSubscriptions = new CompositeSubscription();

    /**
     * 取消订阅
     */
    public void unsubscrbe() {
        mSubscriptions.clear();
    }

    @Override
    public int itemType() {
        return 0;
    }
}
