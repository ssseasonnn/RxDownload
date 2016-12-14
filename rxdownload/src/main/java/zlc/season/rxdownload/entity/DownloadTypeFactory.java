package zlc.season.rxdownload.entity;

import zlc.season.rxdownload.function.DownloadHelper;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 15:21
 * Download Type Factory
 */
public class DownloadTypeFactory {
    private String mUrl;
    private long mFileLength;
    private String mLastModify;
    private DownloadHelper mDownloadHelper;

    public DownloadTypeFactory(DownloadHelper downloadHelper) {
        this.mDownloadHelper = downloadHelper;
    }

    public DownloadTypeFactory url(String url) {
        this.mUrl = url;
        return this;
    }

    public DownloadTypeFactory fileLength(long fileLength) {
        this.mFileLength = fileLength;
        return this;
    }

    public DownloadTypeFactory lastModify(String lastModify) {
        this.mLastModify = lastModify;
        return this;
    }

    public DownloadType buildNormalDownload() {
        DownloadType type = new DownloadType.NormalDownload();
        type.mUrl = this.mUrl;
        type.mFileLength = this.mFileLength;
        type.mLastModify = this.mLastModify;
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType buildContinueDownload() {
        DownloadType type = new DownloadType.ContinueDownload();
        type.mUrl = this.mUrl;
        type.mFileLength = this.mFileLength;
        type.mLastModify = this.mLastModify;
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType buildMultiDownload() {
        DownloadType type = new DownloadType.MultiThreadDownload();
        type.mUrl = this.mUrl;
        type.mFileLength = this.mFileLength;
        type.mLastModify = this.mLastModify;
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType buildAlreadyDownload() {
        DownloadType type = new DownloadType.AlreadyDownloaded();
        type.mUrl = this.mUrl;
        type.mFileLength = this.mFileLength;
        type.mLastModify = this.mLastModify;
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType buildRequestRangeNotSatisfiable() {
        DownloadType type = new DownloadType.RequestRangeNotSatisfiable();
        type.mUrl = this.mUrl;
        type.mFileLength = this.mFileLength;
        type.mLastModify = this.mLastModify;
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }
}
