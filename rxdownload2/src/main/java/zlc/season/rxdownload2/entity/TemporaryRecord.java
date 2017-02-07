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

    private boolean lastModifyReadFlag = false;
    private boolean localFileExistsFlag = false;

    private DownloadType downloadType;

    private DownloadHelper downloadHelper;
    private DownloadTypeFactory downloadTypeFactory;

    public TemporaryRecord(String filePath, String tempPath, String lmfPath,
                           DownloadHelper downloadHelper) {
        this.filePath = filePath;
        this.tempPath = tempPath;
        this.lmfPath = lmfPath;
        this.downloadHelper = downloadHelper;
        downloadTypeFactory = new DownloadTypeFactory(downloadHelper);
    }

    public void setFileExists() {
        this.localFileExistsFlag = true;
    }

    public void setFileNotExists() {
        this.localFileExistsFlag = false;
    }

    public void readLastModifyFailed() {
        this.lastModifyReadFlag = false;
    }

    public void readLastModifySuccess() {
        this.lastModifyReadFlag = true;
    }

    public void setServerFileChangeFlag(int responseCode) {
        this.serverFileChangeFlag = responseCode;
    }

    public void serverFileChanged() {
        this.serverFileChangeFlag = SERVER_FILE_CHANGED;
    }

    public void serverFileNotChange() {
        this.serverFileChangeFlag = SERVER_FILE_NOT_CHANGE;
    }

    public void requestRangeNotSatisfiable() {
        this.serverFileChangeFlag = REQUEST_RANGE_NOT_SATISFIABLE;
    }

    public void unableDownload() {

    }


    public boolean fileHasChanged() {
        return this.serverFileChangeFlag == SERVER_FILE_CHANGED;
    }

    public boolean fileNotChange() {
        return this.serverFileChangeFlag == SERVER_FILE_NOT_CHANGE;
    }

//    public boolean requestRangeNotSatisfiable() {
//        return this.serverFileChangeFlag == REQUEST_RANGE_NOT_SATISFIABLE;
//    }


    public DownloadType fileNotExistsType(DownloadHelper helper) {
        return getDownloadType(helper);
    }

    public DownloadType fileExistsType(DownloadHelper helper) {
        return getFileExistsType(helper);
    }


    private DownloadType getFileExistsType(DownloadHelper helper) {
        DownloadType type;

        if (readLastModifyFailed()) {
            type = getDownloadType(helper);
        } else {
            type = getLastModifyReadSuccessType(helper);
        }
        return type;
    }

    private DownloadType getLastModifyReadSuccessType(DownloadHelper helper) {
        DownloadType type;
        DownloadTypeFactory factory = new DownloadTypeFactory(helper);

        if (fileHasChanged()) {
            type = getDownloadType(helper);
        } else if (fileNotChange()) {
            type = getServerFileNotChangeType(helper);
        } else if (requestRangeNotSatisfiable()) {
            type = factory.useGET(this);
        } else {
            type = factory.unable();
        }
        return type;
    }

    private DownloadType getServerFileNotChangeType(DownloadHelper helper) {
        DownloadType type;
        if (isSupportRange()) {
            type = checkFileStatus(helper);
        } else {
            type = checkFileComplete(helper);
        }
        return type;
    }

    private DownloadType checkFileStatus(DownloadHelper helper) {
        DownloadType type;
        DownloadTypeFactory factory = new DownloadTypeFactory(helper);
        try {
            if (helper.needReDownload(url, contentLength)) {
                type = factory.multithread(this);
            } else if (helper.downloadNotComplete(url)) {
                type = factory.multithread(this);
            } else {
                type = factory.already(this);
            }
        } catch (IOException e) {
            log(DOWNLOAD_RECORD_FILE_DAMAGED);
            type = factory.multithread(this);
        }
        return type;
    }

    private DownloadType checkFileComplete(DownloadHelper helper) {
        DownloadTypeFactory factory = new DownloadTypeFactory(helper);
        if (helper.downloadNotComplete(url, contentLength)) {
            return factory.normal(this);
        } else {
            return factory.already(this);
        }
    }

    private DownloadType getDownloadType(DownloadHelper helper) {
        DownloadType type;
        DownloadTypeFactory factory = new DownloadTypeFactory(helper);

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

    public boolean rangeFlagIsUndefined() {
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
        downloadType = downloadTypeFactory.normal(this);
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
