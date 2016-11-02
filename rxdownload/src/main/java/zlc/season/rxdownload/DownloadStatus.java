package zlc.season.rxdownload;

import java.text.NumberFormat;

import static zlc.season.rxdownload.Utils.formatSize;

/**
 * User: Season(ssseasonnn@gmail.com)
 * Date: 2016-07-15
 * Time: 15:48
 * FIXME
 * 表示下载状态, 如果isChunked为true, totalSize 可能不存在
 */
public class DownloadStatus {
    public boolean isChunked = false;
    private long totalSize;
    private long downloadSize;

    public DownloadStatus() {
    }

    public DownloadStatus(long downloadSize, long totalSize) {
        this.downloadSize = downloadSize;
        this.totalSize = totalSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getDownloadSize() {
        return downloadSize;
    }

    public void setDownloadSize(long downloadSize) {
        this.downloadSize = downloadSize;
    }

    /**
     * 获得格式化的总Size
     *
     * @return example: 2KB , 10MB
     */
    public String getFormatTotalSize() {
        return formatSize(totalSize);
    }

    public String getFormatDownloadSize() {
        return formatSize(downloadSize);
    }

    /**
     * 获得格式化的状态字符串
     *
     * @return example: 2MB/36MB
     */
    public String getFormatStatusString() {
        return getFormatDownloadSize() + "/" + getFormatTotalSize();
    }

    /**
     * 获得下载的百分比, 保留两位小数
     *
     * @return example: 5.25%
     */
    public String getPercent() {
        String percent;
        Double result;
        if (totalSize == 0L) {
            result = 0.0;
        } else {
            result = downloadSize * 1.0 / totalSize;
        }
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(2);//控制保留小数点后几位，2：表示保留2位小数点
        percent = nf.format(result);
        return percent;
    }
}
