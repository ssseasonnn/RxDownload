package zlc.season.rxdownload;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 11:31
 * FIXME
 */
public class DownloadRecord {
    private String mDownloadUrl;
    private DownloadStatus mStatus;
    private String mDate;

    public DownloadRecord() {
    }

    public DownloadRecord(String downloadUrl, DownloadStatus status, String date) {
        mDownloadUrl = downloadUrl;
        mStatus = status;
        mDate = date;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    public DownloadStatus getStatus() {
        return mStatus;
    }

    public void setStatus(DownloadStatus status) {
        mStatus = status;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        mDate = date;
    }
}
