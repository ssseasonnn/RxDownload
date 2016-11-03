package zlc.season.rxdownload;

import java.io.IOException;
import java.text.ParseException;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/3
 * Time: 10:01
 * FIXME
 */
class NormalDownload extends DownloadType {

    @Override
    void prepareDownload() throws IOException, ParseException {
        mFileHelper.prepareNormalDownload(mUrl, mFileLength, mLastModify);
    }

    @Override
    Observable<DownloadStatus> startDownload() {
        return mFileHelper.getDownloadApi().download(null, mUrl)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(final Response<ResponseBody> response) {
                        return normalSave(response);
                    }
                }).onBackpressureLatest().retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return retry(integer, throwable);
                    }
                });
    }

    private Observable<DownloadStatus> normalSave(final Response<ResponseBody> response) {
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                mFileHelper.saveNormalFile(subscriber, mUrl, response);
            }
        });
    }
}