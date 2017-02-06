package zlc.season.rxdownload2.entity;

import java.io.IOException;

import zlc.season.rxdownload2.function.DownloadHelper;

import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_RECORD_FILE_DAMAGED;
import static zlc.season.rxdownload2.function.Utils.log;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/4
 * FIXME
 */
public class TemporaryRecord {
    private static final int UNDEFINED = 0;
    private static final int SUPPORT_RANGE = 1;
    private static final int NOT_SUPPORT_RANGE = 2;

    private static final int SERVER_FILE_CHANGED = 200;
    private static final int SERVER_FILE_NOT_CHANGE = 206;
    private static final int REQUEST_RANGE_NOT_SATISFIABLE = 416;

    private String url;
    private String saveName;
    private String savePath;

    private String filePath;
    private String tempPath;
    private String lmfPath;

    private long contentLength;
    private String lastModify;

    private int rangeSupportFlag = UNDEFINED;
    private int serverFileChangeFlag = UNDEFINED;
    private boolean readLastModifyFailedFlag = true;


    public TemporaryRecord(String filePath, String tempPath, String lmfPath) {
        this.filePath = filePath;
        this.tempPath = tempPath;
        this.lmfPath = lmfPath;
    }

    public void readLastModifyFailed(boolean flag) {
        this.readLastModifyFailedFlag = flag;
    }

    public void setServerFileChangeFlag(int responseCode) {
        this.serverFileChangeFlag = responseCode;
    }

    public boolean fileHasChanged() {
        return this.serverFileChangeFlag == SERVER_FILE_CHANGED;
    }

    public boolean fileNotChange() {
        return this.serverFileChangeFlag == SERVER_FILE_NOT_CHANGE;
    }

    public boolean requestRangeNotSatisfiable() {
        return this.serverFileChangeFlag == REQUEST_RANGE_NOT_SATISFIABLE;
    }


    public DownloadType type(DownloadHelper downloadHelper, boolean fileExists) {
        DownloadTypeFactory factory = new DownloadTypeFactory(downloadHelper);
        DownloadType type;
        if (fileExists) {
            if (readLastModifyFailedFlag) {
                type = getDownloadType(factory);
            } else {
                if (fileHasChanged()) {
                    type = getDownloadType(factory);
                } else if (fileNotChange()) {
                    if (isSupportRange()) {
                        try {
                            if (downloadHelper.needReDownload(url, contentLength)) {
                                type = factory.multithread(this);
                            } else if (downloadHelper.downloadNotComplete(url)) {
                                type = factory.multithread(this);
                            } else {
                                type = factory.already(this);
                            }
                        } catch (IOException e) {
                            log(DOWNLOAD_RECORD_FILE_DAMAGED);
                            type = factory.multithread(this);
                        }
                    } else {
                        if (downloadHelper.downloadNotComplete(url, contentLength)) {
                            return factory.normal(this);
                        } else {
                            return factory.already(this);
                        }
                    }
                } else if (requestRangeNotSatisfiable()) {
                    type = factory.useGET(this);
                } else {
                    type = factory.unable();
                }
            }
        } else {
            type = getDownloadType(factory);
        }
        return type;
    }

    private DownloadType getDownloadType(DownloadTypeFactory factory) {
        DownloadType type;
        if (isSupportRange()) {
            type = factory.multithread(this);
        } else {
            type = factory.normal(this);
        }
        return type;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getLastModify() {
        return lastModify;
    }

    public void setLastModify(String lastModify) {
        this.lastModify = lastModify;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getRangeSupportFlag() {
        return rangeSupportFlag;
    }

    public boolean isRangeUndefined() {
        return rangeSupportFlag == UNDEFINED;
    }

    public boolean isSupportRange() {
        return rangeSupportFlag == SUPPORT_RANGE;
    }

    public void supportRange() {
        this.rangeSupportFlag = SUPPORT_RANGE;
    }

    public void notSupportRange() {
        this.rangeSupportFlag = NOT_SUPPORT_RANGE;
    }

    public String getTempPath() {
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public String getLmfPath() {
        return lmfPath;
    }

    public void setLmfPath(String lmfPath) {
        this.lmfPath = lmfPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
