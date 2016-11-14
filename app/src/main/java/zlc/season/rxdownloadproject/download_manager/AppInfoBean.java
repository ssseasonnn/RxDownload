package zlc.season.rxdownloadproject.download_manager;

import zlc.season.practicalrecyclerview.ItemType;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 09:43
 * FIXME
 */
public class AppInfoBean implements ItemType {
    String name;
    String img;
    String info;
    String downloadUrl;

    public AppInfoBean(String name, String img, String info, String downloadUrl) {
        this.name = name;
        this.img = img;
        this.info = info;
        this.downloadUrl = downloadUrl;
    }

    @Override
    public int itemType() {
        return 0;
    }
}
