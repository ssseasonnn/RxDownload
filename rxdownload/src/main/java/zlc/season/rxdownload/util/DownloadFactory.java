package zlc.season.rxdownload.util;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 15:21
 * Download Type 工厂
 */
public class DownloadFactory {
    private String mUrl;
    private long mFileLength;
    private String mLastModify;
    private DownloadHelper mDownloadHelper;

    public DownloadFactory(DownloadHelper downloadHelper) {
        this.mDownloadHelper = downloadHelper;
    }

    public DownloadFactory url(String url) {
        this.mUrl = url;
        return this;
    }

    public DownloadFactory fileLength(long fileLength) {
        this.mFileLength = fileLength;
        return this;
    }

    public DownloadFactory lastModify(String lastModify) {
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
}
