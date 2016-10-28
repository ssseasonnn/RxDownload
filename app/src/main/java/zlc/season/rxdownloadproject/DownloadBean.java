package zlc.season.rxdownloadproject;

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

    @Override
    public int itemType() {
        return 0;
    }
}
