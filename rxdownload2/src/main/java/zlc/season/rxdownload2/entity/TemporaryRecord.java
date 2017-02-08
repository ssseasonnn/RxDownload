package zlc.season.rxdownload2.entity;

import java.io.File;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/4
 * FIXME
 */
public class TemporaryRecord {
    private String url;
    private String saveName;
    private String savePath;

    private String filePath;
    private String tempPath;
    private String lmfPath;

    private long contentLength;
    private String lastModify;

    private boolean rangeSupport = false;
    /**
     * server file change flag
     */
    private boolean serverFileChanged = false;

    private DownloadType downloadType;

    public TemporaryRecord(String filePath, String tempPath, String lmfPath) {
        this.filePath = filePath;
        this.tempPath = tempPath;
        this.lmfPath = lmfPath;
    }

    public boolean isSupportRange() {
        return rangeSupport;
    }

    public void setRangeSupport(boolean rangeSupport) {
        this.rangeSupport = rangeSupport;
    }

    public DownloadType getDownloadType() {
        return downloadType;
    }

    public void setServerFileChanged(boolean serverFileChanged) {
        this.serverFileChanged = serverFileChanged;
    }

    public void setDownloadType(DownloadType downloadType) {
        this.downloadType = downloadType;
    }

    public boolean isServerFileChanged() {
        return serverFileChanged;
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


    public File getFile() {
        return new File(filePath);
    }

    public File getTempFile() {
        return new File(tempPath);
    }

    public File getLastModifyFile() {
        return new File(lmfPath);
    }

    public boolean fileExists() {
        return getFile().exists();
    }

    public boolean tempExists() {
        return getTempFile().exists();
    }

    public boolean fileComplete() {
        return getFile().length() == contentLength;
    }

}
