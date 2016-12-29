package zlc.season.rxdownload.entity;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/21
 * Time: 15:28
 * Download Range
 */
public class DownloadRange {
    public long start;
    public long end;

    public DownloadRange(long start, long end) {
        this.start = start;
        this.end = end;
    }
}
