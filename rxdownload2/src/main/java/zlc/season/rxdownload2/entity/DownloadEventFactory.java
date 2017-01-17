package zlc.season.rxdownload2.entity;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import static zlc.season.rxdownload2.entity.DownloadFlag.COMPLETED;
import static zlc.season.rxdownload2.entity.DownloadFlag.FAILED;
import static zlc.season.rxdownload2.entity.DownloadFlag.NORMAL;
import static zlc.season.rxdownload2.entity.DownloadFlag.STARTED;
import static zlc.season.rxdownload2.entity.DownloadFlag.WAITING;

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

    public DownloadEvent normal(String url) {
        return create(url, NORMAL, null);
    }

    public DownloadEvent waiting(String url) {
        return create(url, WAITING, null);
    }

    public DownloadEvent waiting(String url, DownloadStatus status) {
        return create(url, WAITING, status);
    }

    public DownloadEvent started(String url, DownloadStatus status) {
        return create(url, STARTED, status);
    }

    public DownloadEvent completed(String url, DownloadStatus status) {
        return create(url, COMPLETED, status);
    }

    public DownloadEvent failed(String url, DownloadStatus status, Throwable throwable) {
        return create(url, FAILED, status, throwable);
    }

    public DownloadEvent create(String url, int flag, DownloadStatus status) {
        DownloadEvent event = createEvent(url, flag, status);
        event.setError(null);
        return event;
    }

    public DownloadEvent create(String url, int flag, DownloadStatus status, Throwable throwable) {
        DownloadEvent event = createEvent(url, flag, status);
        event.setError(throwable);
        return event;
    }

    @NonNull
    private DownloadEvent createEvent(String url, int flag, DownloadStatus status) {
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
