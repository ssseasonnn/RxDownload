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
    public static final int UNDETERMINED = 0;
    public static final int SUPPORT = 1;
    public static final int NOT_SUPPORT = 2;

    private String url;
    private String saveName;
    private String savePath;

    private String filePath;
    private String tempPath;
    private String lmfPath;

    private long contentLength;
    private String lastModify;

    private int rangeAbility = UNDETERMINED;

    private boolean supportHeadMethod = true;

    private boolean serverFileChangeState = false;
    private boolean lastModifyReadState = false;
    private boolean localFileExists = false;

    private DownloadType downloadType;

    public TemporaryRecord(String filePath, String tempPath, String lmfPath) {
        this.filePath = filePath;
        this.tempPath = tempPath;
        this.lmfPath = lmfPath;
    }

    public boolean isSupportHeadMethod() {
        return supportHeadMethod;
    }

    public void setSupportHeadMethod(boolean supportHeadMethod) {
        this.supportHeadMethod = supportHeadMethod;
    }

    public int getRangeAbility() {
        return rangeAbility;
    }

    public void setRangeAbility(int rangeAbility) {
        this.rangeAbility = rangeAbility;
    }

    public DownloadType getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(DownloadType downloadType) {
        this.downloadType = downloadType;
    }

    public boolean isServerFileChanged() {
        return serverFileChangeState;
    }

    public boolean isLastModifyReadSuccess() {
        return lastModifyReadState;
    }

    public boolean isLocalFileExists() {
        return localFileExists;
    }

    public void setLocalFileExists(boolean localFileExists) {
        this.localFileExists = localFileExists;
    }

    public void setServerFileChangeState(boolean serverFileChangeState) {
        this.serverFileChangeState = serverFileChangeState;
    }

    public void setLastModifyReadState(boolean lastModifyReadState) {
        this.lastModifyReadState = lastModifyReadState;
    }

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
