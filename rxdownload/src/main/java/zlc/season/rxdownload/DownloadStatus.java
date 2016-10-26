package zlc.season.rxdownload;

/**
 * User: Season(ssseasonnn@gmail.com)
 * Date: 2016-07-15
 * Time: 15:48
 * FIXME
 */
public class DownloadStatus {
    public long totalSize;
    public long downloadSize;
    public boolean isChuncked = false;

    public DownloadStatus() {
    }

    public DownloadStatus(long downloadSize, long totalSize) {
        this.downloadSize = downloadSize;
        this.totalSize = totalSize;
    }
}
