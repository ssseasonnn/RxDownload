package zlc.season.rxdownload2.entity;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/22
 * Time: 10:49
 * FIXME
 */
public class DownloadEvent {
    private int flag = DownloadFlag.NORMAL;
    private DownloadStatus downloadStatus = new DownloadStatus();
    private Throwable mError;

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public Throwable getError() {
        return mError;
    }

    public void setError(Throwable error) {
        mError = error;
    }
}
