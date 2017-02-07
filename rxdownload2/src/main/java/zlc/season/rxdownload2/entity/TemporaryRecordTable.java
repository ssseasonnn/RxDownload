package zlc.season.rxdownload2.entity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;
import zlc.season.rxdownload2.function.DownloadHelper;

import static zlc.season.rxdownload2.entity.TemporaryRecord.NOT_SUPPORT;
import static zlc.season.rxdownload2.entity.TemporaryRecord.SUPPORT;
import static zlc.season.rxdownload2.entity.TemporaryRecord.UNDETERMINED;
import static zlc.season.rxdownload2.function.Utils.contentLength;
import static zlc.season.rxdownload2.function.Utils.empty;
import static zlc.season.rxdownload2.function.Utils.fileName;
import static zlc.season.rxdownload2.function.Utils.lastModify;
import static zlc.season.rxdownload2.function.Utils.notSupportRange;
import static zlc.season.rxdownload2.function.Utils.requestRangeNotSatisfiable;
import static zlc.season.rxdownload2.function.Utils.serverFileNotChange;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/4
 * FIXME
 */
public class TemporaryRecordTable {
    private Map<String, TemporaryRecord> map;

    private DownloadHelper downloadHelper;
    private DownloadTypeFactory downloadTypeFactory;

    public TemporaryRecordTable(DownloadHelper downloadHelper) {
        this.map = new HashMap<>();
        this.downloadHelper = downloadHelper;
        this.downloadTypeFactory = new DownloadTypeFactory(downloadHelper);
    }

    public void add(String url, TemporaryRecord record) {
        map.put(url, record);
    }


    public void setFileExists(String url) {
        map.get(url).setLocalFileExists(true);
    }

    public void setFileNotExists(String url) {
        map.get(url).setLocalFileExists(false);
    }

    public void setLastModifyReadFailed(String url) {
        map.get(url).setLastModifyReadState(false);
    }

    public void setLastModifyReadSuccess(String url) {
        map.get(url).setLastModifyReadState(true);
    }

    public boolean neverQuery(String url) {
        return map.get(url).getRangeAbility() == UNDETERMINED;
    }

    public void updateExtraInfo(String url, Response<?> response) {
        saveHttpInfo(url, response);

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

    public boolean notSupportHeadMethod(String url) {
        return !map.get(url).isSupportHeadMethod();
    }

    public void saveHttpInfo(String url, Response<?> response) {
        TemporaryRecord record = map.get(url);
        if (response.isSuccessful()) {
            record.setSupportHeadMethod(true);
        } else {
            record.setSupportHeadMethod(false);
        }

        if (notSupportRange(response)) {
            record.setRangeAbility(NOT_SUPPORT);
        } else {
            record.setRangeAbility(SUPPORT);
        }

        if (empty(record.getSaveName())) {
            record.setSaveName(fileName(url, response));
        }
        record.setContentLength(contentLength(response));
        record.setLastModify(lastModify(response));
    }

    public String getSaveName(String url) {
        return map.get(url).getSaveName();
    }

    public boolean contain(String url) {
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

    public DownloadType getType(String url) {
        return map.get(url).getDownloadType();
    }

    private boolean supportRange(String url) {
        return map.get(url).getRangeAbility() == SUPPORT;
    }

    private void setType(String url, DownloadType type) {
        map.get(url).setDownloadType(type);
    }

    public void generateFileNotExistsType(String url) {
        DownloadType type = generateCommonType(url);
        setType(url, type);
    }

    private DownloadType generateCommonType(String url) {
        DownloadType type;
        if (supportRange(url)) {
            type = downloadTypeFactory.multithread(map.get(url));
        } else {
            type = downloadTypeFactory.normal(map.get(url));
        }
        return type;
    }

    private boolean readLastModifySuccess(String url) {
        return map.get(url).isLastModifyReadSuccess();
    }

    private boolean serverFileChanged(String url) {
        return map.get(url).isServerFileChanged();
    }


    public void generateFileExistsType(String url) {
        DownloadType type;
        if (readLastModifySuccess(url)) {
            if (serverFileChanged(url)) {
                type = generateCommonType(url);
            } else if (fileNotChange()) {
                type = getServerFileNotChangeType(helper);
            } else if (requestRangeNotSatisfiable()) {
                type = factory.useGET(this);
            } else {
                type = factory.unable();
            }
        } else {
            type = getLastModifyReadSuccessType(helper);
        }
        return type;
    }

    public void saveFileState(String url, Response<Void> response) {
        if (response.code() == 304) {
            //TODO file not change
        } else if (response.code() == 200) {
            //TODO file has changed
        } else {

        }
    }
}
