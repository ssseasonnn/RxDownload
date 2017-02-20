package zlc.season.rxdownload2.function;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;
import zlc.season.rxdownload2.db.DataBaseHelper;
import zlc.season.rxdownload2.entity.DownloadType;
import zlc.season.rxdownload2.entity.DownloadType.AlreadyDownloaded;
import zlc.season.rxdownload2.entity.DownloadType.ContinueDownload;
import zlc.season.rxdownload2.entity.DownloadType.MultiThreadDownload;
import zlc.season.rxdownload2.entity.DownloadType.NormalDownload;
import zlc.season.rxdownload2.entity.TemporaryRecord;

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

    /**
     * Save file info
     *
     * @param url      key
     * @param response response
     */
    public void saveFileInfo(String url, Response<?> response) {
        TemporaryRecord record = map.get(url);
        if (empty(record.getSaveName())) {
            record.setSaveName(fileName(url, response));
        }
        record.setContentLength(contentLength(response));
        record.setLastModify(lastModify(response));
    }

    /**
     * Save range info
     *
     * @param url      key
     * @param response response
     */
    public void saveRangeInfo(String url, Response<?> response) {
        map.get(url).setRangeSupport(!notSupportRange(response));
    }

    /**
     * Init necessary info
     *
     * @param url             url
     * @param maxThreads      max threads
     * @param maxRetryCount   retry count
     * @param defaultSavePath default save path
     * @param downloadApi     api
     * @param dataBaseHelper  DataBaseHelper
     */
    public void init(String url, int maxThreads, int maxRetryCount, String defaultSavePath,
                     DownloadApi downloadApi, DataBaseHelper dataBaseHelper) {
        map.get(url).init(maxThreads, maxRetryCount, defaultSavePath, downloadApi, dataBaseHelper);
    }

    /**
     * Save file state, change or not change.
     *
     * @param url      key
     * @param response response
     */
    public void saveFileState(String url, Response<Void> response) {
        if (response.code() == 304) {
            map.get(url).setFileChanged(false);
        } else if (response.code() == 200) {
            map.get(url).setFileChanged(true);
        }
    }

    /**
     * return file not exists download type.
     *
     * @param url key
     * @return download type
     */
    public DownloadType generateNonExistsType(String url) {
        return getNormalType(url);
    }

    /**
     * return file exists download type
     *
     * @param url key
     * @return download type
     */
    public DownloadType generateFileExistsType(String url) {
        DownloadType type;
        if (fileChanged(url)) {
            type = getNormalType(url);
        } else {
            type = getServerFileChangeType(url);
        }
        return type;
    }

    /**
     * read last modify string
     *
     * @param url key
     * @return last modify
     */
    public String readLastModify(String url) {
        try {
            return map.get(url).readLastModify();
        } catch (IOException e) {
            //TODO log
            //If read failed,return an empty string.
            //If we send empty last-modify,server will response 200.
            //That means file changed.
            return "";
        }
    }

    public boolean fileExists(String url) {
        return map.get(url).file().exists();
    }

    public File[] getFiles(String url) {
        return map.get(url).getFiles();
    }

    private boolean supportRange(String url) {
        return map.get(url).isSupportRange();
    }

    private boolean fileChanged(String url) {
        return map.get(url).isFileChanged();
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
            return supportRangeType(url);
        } else {
            return notSupportRangeType(url);
        }
    }

    private DownloadType supportRangeType(String url) {
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

    private DownloadType notSupportRangeType(String url) {
        if (normalDownloadNotComplete(url)) {
            return new NormalDownload(map.get(url));
        } else {
            return new AlreadyDownloaded(map.get(url));
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

    private boolean tempFileDamaged(String url) {
        try {
            return map.get(url).tempFileDamaged();
        } catch (IOException e) {
            log(DOWNLOAD_RECORD_FILE_DAMAGED);
            return true;
        }
    }

    private boolean tempFileNotExists(String url) {
        return !map.get(url).tempFile().exists();
    }
}
