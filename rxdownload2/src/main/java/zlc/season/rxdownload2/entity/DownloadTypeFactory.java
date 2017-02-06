package zlc.season.rxdownload2.entity;

import zlc.season.rxdownload2.function.DownloadHelper;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 15:21
 * Download Type Factory
 */
public class DownloadTypeFactory {
    private DownloadHelper mDownloadHelper;

    public DownloadTypeFactory(DownloadHelper downloadHelper) {
        this.mDownloadHelper = downloadHelper;
    }

    public DownloadType normal(TemporaryRecord record) {
        DownloadType type = new DownloadType.NormalDownload();
        type.mUrl = record.getUrl();
        type.mFileLength = record.getContentLength();
        type.mLastModify = record.getLastModify();
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType continued(TemporaryRecord record) {
        DownloadType type = new DownloadType.ContinueDownload();
        type.mUrl = record.getUrl();
        type.mFileLength = record.getContentLength();
        type.mLastModify = record.getLastModify();
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType multithread(TemporaryRecord record) {
        DownloadType type = new DownloadType.MultiThreadDownload();
        type.mUrl = record.getUrl();
        type.mFileLength = record.getContentLength();
        type.mLastModify = record.getLastModify();
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType already(TemporaryRecord record) {
        DownloadType type = new DownloadType.AlreadyDownloaded();
        type.mUrl = record.getUrl();
        type.mFileLength = record.getContentLength();
        type.mLastModify = record.getLastModify();
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType useGET(TemporaryRecord record) {
        DownloadType type = new DownloadType.NotSupportHEAD();
        type.mUrl = record.getUrl();
        type.mFileLength = record.getContentLength();
        type.mLastModify = record.getLastModify();
        type.mDownloadHelper = this.mDownloadHelper;
        return type;
    }

    public DownloadType unable(){
        return new DownloadType.UnableDownload();
    }
}
