package zlc.season.rxdownload.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/23
 * Time: 14:18
 * FIXME
 */
public class DownloadEventFactory {
    private volatile static DownloadEventFactory singleton;
    private Map<String, DownloadEvent> map = new HashMap<>();

    private DownloadEventFactory() {
    }

    public static DownloadEventFactory getSingleton() {
        if (singleton == null) {
            synchronized (DownloadEventFactory.class) {
                if (singleton == null) {
                    singleton = new DownloadEventFactory();
                }
            }
        }
        return singleton;
    }

    public DownloadEvent factory(String url, int flag, DownloadStatus status) {
        DownloadEvent event = map.get(url);
        if (event == null) {
            event = new DownloadEvent();
            map.put(url, event);
        }
        event.setDownloadStatus(status == null ? new DownloadStatus() : status);
        event.setFlag(flag);
        return event;
    }
}
