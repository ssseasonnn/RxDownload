package zlc.season.rxdownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;

import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import static zlc.season.rxdownload.DownloadHelper.TAG;
import static zlc.season.rxdownload.DownloadHelper.TEST_RANGE_SUPPORT;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:46
 * RxDownload
 */
public class RxDownload {
    private DownloadHelper mDownloadHelper;
    private DownloadFactory mFactory;

    private RxDownload() {
        mDownloadHelper = new DownloadHelper();
        mFactory = new DownloadFactory(mDownloadHelper);
    }

    public static RxDownload getInstance() {
        return new RxDownload();
    }

    public RxDownload defaultSavePath(String savePath) {
        mDownloadHelper.setDefaultSavePath(savePath);
        return this;
    }

    public RxDownload retrofit(Retrofit retrofit) {
        mDownloadHelper.setRetrofit(retrofit);
        return this;
    }

    public RxDownload maxThread(int max) {
        mDownloadHelper.setMaxThreads(max);
        return this;
    }

    public RxDownload maxRetryCount(int max) {
        mDownloadHelper.setMaxRetryCount(max);
        return this;
    }

    /**
     * 开始下载
     *
     * @param url      下载文件的Url
     * @param saveName 下载文件的保存名称
     * @param savePath 下载文件的保存路径, null使用默认的路径,默认保存在/sdcard/Download/目录下
     * @return Observable
     */
    public Observable<DownloadStatus> download(@NonNull final String url, @NonNull final String saveName,
                                               @Nullable final String savePath) {
        return downloadDispatcher(url, saveName, savePath);
    }

    private Observable<DownloadStatus> downloadDispatcher(final String url, final String saveName,
                                                          final String savePath) {
        mDownloadHelper.addDownloadRecord(url, saveName, savePath);
        return getDownloadType(url)
                .flatMap(new Func1<DownloadType, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(DownloadType type) {
                        try {
                            type.prepareDownload();
                        } catch (IOException | ParseException e) {
                            return Observable.error(e);
                        }
                        try {
                            return type.startDownload();
                        } catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        mDownloadHelper.deleteDownloadRecord(url);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mDownloadHelper.deleteDownloadRecord(url);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.w(TAG, throwable);
                        mDownloadHelper.deleteDownloadRecord(url);
                    }
                });
    }

    private Observable<DownloadType> getDownloadType(String url) {
        if (mDownloadHelper.getFileBy(url).exists()) {
            try {
                return getWhenFileExists(url);
            } catch (IOException e) {
                return getWhenFileNotExists(url);
            }
        } else {
            return getWhenFileNotExists(url);
        }
    }

    private Observable<DownloadType> getWhenFileNotExists(@NonNull final String url) {
        return mDownloadHelper.getDownloadApi().getHttpHeader(TEST_RANGE_SUPPORT, url)
                .map(new Func1<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType call(Response<Void> response) {
                        if (Utils.notSupportRange(response)) {
                            return mFactory.url(url).fileLength(Utils.contentLength(response))
                                    .lastModify(Utils.lastModify(response))
                                    .buildNormalDownload();
                        } else {
                            return mFactory.url(url).lastModify(Utils.lastModify(response))
                                    .fileLength(Utils.contentLength(response))
                                    .buildMultiDownload();
                        }
                    }
                }).retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return mDownloadHelper.retry(integer, throwable);
                    }
                });
    }

    private Observable<DownloadType> getWhenFileExists(final String url) throws IOException {
        return mDownloadHelper.getDownloadApi()
                .getHttpHeaderWithIfRange(TEST_RANGE_SUPPORT, mDownloadHelper.getLastModify(url), url)
                .map(new Func1<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType call(Response<Void> resp) {
                        if (resp.code() == 206) {
                            //server file no changed
                            return getWhen206(resp, url);
                        } else {
                            //server file has changed, need re download
                            return getWhen200(resp, url);
                        }
                    }
                }).retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return mDownloadHelper.retry(integer, throwable);
                    }
                });
    }

    private DownloadType getWhen200(Response<Void> resp, String url) {
        if (Utils.notSupportRange(resp)) {
            return mFactory.url(url).fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp)).buildNormalDownload();
        } else {
            return mFactory.url(url).fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp)).buildMultiDownload();
        }
    }

    private DownloadType getWhen206(Response<Void> resp, String url) {
        if (Utils.notSupportRange(resp)) {
            return getWhenNotSupportRange(resp, url);
        } else {
            return getWhenSupportRange(resp, url);
        }
    }

    private DownloadType getWhenSupportRange(Response<Void> resp, String url) {
        long contentLength = Utils.contentLength(resp);
        try {
            if (mDownloadHelper.tempFileNotExists(url) || mDownloadHelper.tempFileDamaged(url, contentLength)) {
                return mFactory.url(url).fileLength(contentLength).lastModify(Utils.lastModify(resp))
                        .buildMultiDownload();
            }
            if (mDownloadHelper.downloadNotComplete(url)) {
                return mFactory.url(url).fileLength(contentLength).lastModify(Utils.lastModify(resp))
                        .buildContinueDownload();
            }
        } catch (IOException e) {
            Log.w(TAG, "download record file may be damaged,so we will re download");
            return mFactory.url(url).fileLength(contentLength).lastModify(Utils.lastModify(resp)).buildMultiDownload();
        }
        return mFactory.fileLength(contentLength).buildAlreadyDownload();
    }

    private DownloadType getWhenNotSupportRange(Response<Void> resp, String url) {
        long contentLength = Utils.contentLength(resp);
        if (mDownloadHelper.getFileBy(url).length() == contentLength) {
            return mFactory.fileLength(contentLength).buildAlreadyDownload();
        } else {
            return mFactory.url(url).fileLength(contentLength).lastModify(Utils.lastModify(resp)).buildNormalDownload();
        }
    }
}
