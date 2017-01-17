package zlc.season.rxdownloadproject.download_manager;

import zlc.season.practicalrecyclerview.ItemType;
import zlc.season.rxdownload2.entity.DownloadRecord;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/28
 * Time: 09:30
 * FIXME
 */
public class DownloadBean implements ItemType {
    DownloadRecord mRecord;

    @Override
    public int itemType() {
        return 0;
    }
}
