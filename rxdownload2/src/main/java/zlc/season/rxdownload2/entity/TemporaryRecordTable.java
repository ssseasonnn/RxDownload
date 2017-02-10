package zlc.season.rxdownload2.entity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;
import zlc.season.rxdownload2.entity.DownloadType.AlreadyDownloaded;
import zlc.season.rxdownload2.entity.DownloadType.ContinueDownload;
import zlc.season.rxdownload2.entity.DownloadType.MultiThreadDownload;
import zlc.season.rxdownload2.entity.DownloadType.NormalDownload;
import zlc.season.rxdownload2.function.DownloadApi;

import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_RECORD_FILE_DAMAGED;
import static zlc.season.rxdownload2.function.Utils.contentLength;
import static zlc.season.rxdownload2.function.Utils.empty;
import static zlc.season.rxdownload2.function.Utils.fileName;
import static zlc.season.rxdownload2.function.Utils.lastModify;
import static zlc.season.rxdownload2.function.Utils.log;
import static zlc.season.rxdownload2.function.Utils.notSupportRange;

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

    public boolean contain(String url) {
        return map.get(url) != null;
    }

    public void delete(String url) {
        map.remove(url);
    }

    public void saveFileInfo(String url, Response<?> response) {
        TemporaryRecord record = map.get(url);
        if (empty(record.getSaveName())) {
            record.setSaveName(fileName(url, response));
        }
        record.setContentLength(contentLength(response));
        record.setLastModify(lastModify(response));
    }

    public void initialize(String url, int maxRetryCount, int maxThreads,
                           String defaultSavePath, DownloadApi downloadApi) {
        map.get(url).initializeEnvironment(maxRetryCount, maxThreads, defaultSavePath, downloadApi);
    }

    public void saveRangeInfo(String url, Response<?> response) {
        map.get(url).setRangeSupport(!notSupportRange(response));
    }

    public void saveServerFileState(String url, Response<Void> response) {
        if (response.code() == 304) {
            map.get(url).setServerFileChanged(false);
        } else if (response.code() == 200) {
            map.get(url).setServerFileChanged(true);
        }
    }

    private boolean supportRange(String url) {
        return map.get(url).isSupportRange();
    }

    private boolean serverFileChanged(String url) {
        return map.get(url).isServerFileChanged();
    }

    public DownloadType getDownloadType(String url) {
        return map.get(url).getDownloadType();
    }

    private void setDownloadType(String url, DownloadType type) {
        map.get(url).setDownloadType(type);
    }

    public void generateFileNotExistsType(String url) {
        DownloadType type = getNormalType(url);
        setDownloadType(url, type);
    }

    public void generateFileExistsType(String url) {
        DownloadType type;
        if (serverFileChanged(url)) {
            type = getNormalType(url);
        } else {
            type = getServerFileChangeType(url);
        }
        setDownloadType(url, type);
    }

    private DownloadType getNormalType(String url) {
        DownloadType type;
        if (supportRange(url)) {
            type = new MultiThreadDownload(map.get(url));
        } else {
            type = new NormalDownload(map.get(url));
        }
        return type;
    }

    private DownloadType getServerFileChangeType(String url) {
        if (supportRange(url)) {
            return getSupportRangeType(url);
        } else {
            return getNotSupportRangeType(url);
        }
    }

    private DownloadType getSupportRangeType(String url) {
        if (needReDownload(url)) {
            return new MultiThreadDownload(map.get(url));
        }
        try {
            if (multiDownloadNotComplete(url)) {
                return new ContinueDownload(map.get(url));
            }
        } catch (IOException e) {
            return new MultiThreadDownload(map.get(url));
        }
        return new AlreadyDownloaded(map.get(url));
    }


    private DownloadType getNotSupportRangeType(String url) {
        if (normalDownloadNotComplete(url)) {
            return new NormalDownload(map.get(url));
        } else {
            return new AlreadyDownloaded(map.get(url));
        }
    }

    public String readLastModify(String url) {
        try {
            return map.get(url).readLastModify();
        } catch (IOException e) {
            //TODO log
            return "";
        }
    }

    private boolean multiDownloadNotComplete(String url) throws IOException {
        return map.get(url).fileNotComplete();
    }

    private boolean normalDownloadNotComplete(String url) {
        return !map.get(url).fileComplete();
    }

    private boolean needReDownload(String url) {
        return tempFileNotExists(url) || tempFileDamaged(url);
    }

    public boolean fileExists(String url) {
        return map.get(url).fileExists();
    }

    private boolean tempFileDamaged(String url) {
        try {
            return map.get(url).tempFileDamaged();
        } catch (IOException e) {
            log(DOWNLOAD_RECORD_FILE_DAMAGED);
            return true;
        }
    }

    private boolean tempFileNotExists(String url) {
        return !map.get(url).tempExists();
    }

    public File[] getFiles(String url) {
        return map.get(url).getFiles();
    }
}
