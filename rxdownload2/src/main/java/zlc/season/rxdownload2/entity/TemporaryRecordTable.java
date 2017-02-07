package zlc.season.rxdownload2.entity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;

import static zlc.season.rxdownload2.function.Utils.contentDisposition;
import static zlc.season.rxdownload2.function.Utils.contentLength;
import static zlc.season.rxdownload2.function.Utils.empty;
import static zlc.season.rxdownload2.function.Utils.lastModify;
import static zlc.season.rxdownload2.function.Utils.notSupportRange;
import static zlc.season.rxdownload2.function.Utils.requestRangeNotSatisfiable;
import static zlc.season.rxdownload2.function.Utils.serverFileChanged;
import static zlc.season.rxdownload2.function.Utils.serverFileNotChange;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/4
 * FIXME
 */
public class TemporaryRecordTable {
    private Map<String, TemporaryRecord> map;

    public TemporaryRecordTable() {
        this.map = new HashMap<>();
    }

    public void add(String url, TemporaryRecord record) {
        map.put(url, record);
    }

    public void updateExtraInfo(String url, Response<?> response) {
        updateBaseInfo(url, response);

        TemporaryRecord record = map.get(url);
        if (serverFileChanged(response)) {
            record.serverFileChanged();
        } else if (serverFileNotChange(response)) {
            record.serverFileNotChange();
        } else if (requestRangeNotSatisfiable(response)) {
            record.requestRangeNotSatisfiable();
        } else {
            record.unableDownload();
        }
    }


    public void updateBaseInfo(String url, Response<?> response) {
        String fileName = contentDisposition(response);
        if (empty(fileName)) {
            fileName = url.substring(url.lastIndexOf("/"));
        }

        TemporaryRecord record = map.get(url);
        record.setSaveName(fileName);

        if (notSupportRange(response)) {
            record.notSupportRange();
        } else {
            record.supportRange();
        }
        record.setContentLength(contentLength(response));
        record.setLastModify(lastModify(response));
    }

    public TemporaryRecord get(String url) {
        return map.get(url);
    }

    public boolean exists(String url) {
        return map.get(url) != null;
    }

    public void delete(String url) {
        map.remove(url);
    }

    public File getFile(String url) {
        return new File(map.get(url).getFilePath());
    }

    public File getTempFile(String url) {
        return new File(map.get(url).getTempPath());
    }

    public File getLastModifyFile(String url) {
        return new File(map.get(url).getLmfPath());
    }
}
