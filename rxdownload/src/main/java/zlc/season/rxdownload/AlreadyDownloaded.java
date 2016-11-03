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
class AlreadyDownloaded extends DownloadType {

    @Override
    void prepareDownload() throws IOException, ParseException {

    }

    @Override
    Observable<DownloadStatus> startDownload() throws IOException {
        return Observable.just(new DownloadStatus(mFileLength, mFileLength));
    }
}