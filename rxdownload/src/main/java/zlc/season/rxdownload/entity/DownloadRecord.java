package zlc.season.rxdownload.entity;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 11:31
 * FIXME
 */
public class DownloadRecord {
    private String url;
    private String saveName;
    private String savePath;  //param path, not file save path;
    private DownloadStatus mStatus;
    private int downloadFlag = DownloadEvent.FlagHolder.NORMAL;
    private long date; //格林威治时间,毫秒

    public DownloadRecord() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public DownloadStatus getStatus() {
        return mStatus;
    }

    public void setStatus(DownloadStatus status) {
        mStatus = status;
    }

    public int getDownloadFlag() {
        return downloadFlag;
    }

    public void setDownloadFlag(int downloadFlag) {
        this.downloadFlag = downloadFlag;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
