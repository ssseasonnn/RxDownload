package zlc.season.rxdownload;

import java.io.IOException;
import java.text.ParseException;

import rx.Observable;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 10:03
 * FIXME
 */
class MultiThreadDownload extends ContinueDownload {

    @Override
    void prepareDownload() throws IOException, ParseException {
        mFileHelper.prepareMultiThreadDownload(mFilePath, mFileLength, mLastModify);
    }

    @Override
    Observable<DownloadStatus> startDownload() throws IOException {
        return super.startDownload();
    }
}
