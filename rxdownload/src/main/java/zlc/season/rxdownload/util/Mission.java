package zlc.season.rxdownload.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import rx.subjects.Subject;
import zlc.season.rxdownload.db.DataBaseHelper;
import zlc.season.rxdownload.entity.DownloadStatus;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/21
 * Time: 09:54
 * FIXME
 */
public interface Mission {
    void init(Map<String, Subject<DownloadStatus, DownloadStatus>> subjectPool,
              final AtomicInteger count, final DataBaseHelper db);

    void start();

    void pause();

    void cancel();
}
