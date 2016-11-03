package zlc.season.rxdownload;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/21
 * Time: 15:28
 * Download Range
 */
class DownloadRange {
    long[] start;
    long[] end;

    DownloadRange(long[] start, long[] end) {
        this.start = start;
        this.end = end;
    }
}
