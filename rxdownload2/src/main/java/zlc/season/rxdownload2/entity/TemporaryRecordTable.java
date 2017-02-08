package zlc.season.rxdownload2.entity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;
import zlc.season.rxdownload2.function.DownloadHelper;
import zlc.season.rxdownload2.function.FileHelper;

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

    private FileHelper fileHelper;
    private DownloadHelper downloadHelper;
    private DownloadTypeFactory downloadTypeFactory;

    public TemporaryRecordTable(DownloadHelper downloadHelper) {
        this.map = new HashMap<>();
        this.downloadHelper = downloadHelper;
        this.fileHelper = downloadHelper.getFileHelper();
        this.downloadTypeFactory = new DownloadTypeFactory(downloadHelper);
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
            type = downloadTypeFactory.multithread(map.get(url));
        } else {
            type = downloadTypeFactory.normal(map.get(url));
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
            return downloadTypeFactory.multithread(map.get(url));
        }
        try {
            if (multiDownloadNotComplete(url)) {
                return downloadTypeFactory.continued(map.get(url));
            }
        } catch (IOException e) {
            return downloadTypeFactory.multithread(map.get(url));
        }
        return downloadTypeFactory.already(map.get(url));
    }


    private DownloadType getNotSupportRangeType(String url) {
        if (normalDownloadNotComplete(url)) {
            return downloadTypeFactory.normal(map.get(url));
        } else {
            return downloadTypeFactory.already(map.get(url));
        }
    }

    public String readLastModify(String url) {
        try {
            return fileHelper.getLastModify(map.get(url).getFile());
        } catch (IOException e) {
            //TODO log
            return "";
        }
    }

    private boolean multiDownloadNotComplete(String url) throws IOException {
        return fileHelper.fileNotComplete(map.get(url).getTempFile());
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
            return fileHelper.tempFileDamaged(map.get(url).getTempFile(), map.get(url).getContentLength());
        } catch (IOException e) {
            log(DOWNLOAD_RECORD_FILE_DAMAGED);
            return true;
        }
    }

    private boolean tempFileNotExists(String url) {
        return !map.get(url).tempExists();
    }
}
