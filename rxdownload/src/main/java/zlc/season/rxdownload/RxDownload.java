package zlc.season.rxdownload;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;

import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:46
 * FIXME
 */
public class RxDownload {
    private static final String TAG = "RxDownload";
    private static final String TEST_RANGE_SUPPORT = "bytes=0-";

    private DownloadApi mDownloadApi;
    private Retrofit mRetrofit;

    private FileHelper mFileHelper;
    private DownloadType.Builder mBuilder;

    private RxDownload() {
        mFileHelper = new FileHelper();
        mBuilder = new DownloadType.Builder(mFileHelper);
    }

    public static RxDownload getInstance() {
        return new RxDownload();
    }

    public RxDownload defaultSavePath(String savePath) {
        mFileHelper.setDefaultPath(savePath);
        return this;
    }

    public RxDownload retrofit(Retrofit retrofit) {
        this.mRetrofit = retrofit;
        return this;
    }

    public RxDownload maxThread(int max) {
        mFileHelper.setMaxThreads(max);
        return this;
    }

    public RxDownload maxRetryCount(int max) {
        mFileHelper.setMaxRetryCount(max);
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
        beforeDownload(url, saveName, savePath);
        return downloadDispatcher(url);
    }

    private Observable<DownloadStatus> downloadDispatcher(final String url) {
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
                }).doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.w(TAG, throwable);
                    }
                });
    }

    private Observable<DownloadType> getDownloadType(String url) {
        if (mFileHelper.getFile(url).exists()) {
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
        return mDownloadApi.getHttpHeader(TEST_RANGE_SUPPORT, url)
                .map(new Func1<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType call(Response<Void> response) {
                        if (Utils.notSupportRange(response)) {
                            return mBuilder.url(url).fileLength(Utils.contentLength(response))
                                    .lastModify(Utils.lastModify(response))
                                    .buildNormalDownload();
                        } else {
                            return mBuilder.url(url).lastModify(Utils.lastModify(response))
                                    .fileLength(Utils.contentLength(response))
                                    .buildMultiDownload();
                        }
                    }
                });
    }

    private Observable<DownloadType> getWhenFileExists(final String url) throws IOException {
        return mDownloadApi.getHttpHeaderWithIfRange(TEST_RANGE_SUPPORT, mFileHelper.getLastModify(url), url)
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
                });
    }

    private DownloadType getWhen200(Response<Void> resp, String url) {
        if (Utils.notSupportRange(resp)) {
            return mBuilder.url(url).fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp)).buildNormalDownload();
        } else {
            return mBuilder.url(url).fileLength(Utils.contentLength(resp))
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
            if (mFileHelper.recordFileNotExists(url) || mFileHelper.recordFileDamaged(url, contentLength)) {
                return mBuilder.url(url).fileLength(contentLength).lastModify(Utils.lastModify(resp))
                        .buildMultiDownload();
            }
            if (mFileHelper.downloadNotComplete(url)) {
                return mBuilder.url(url).fileLength(contentLength).lastModify(Utils.lastModify(resp))
                        .buildContinueDownload();
            }
        } catch (IOException e) {
            Log.w(TAG, "download record file may be damaged,so we will re download");
            return mBuilder.url(url).fileLength(contentLength).lastModify(Utils.lastModify(resp)).buildMultiDownload();
        }
        return mBuilder.fileLength(contentLength).buildAlreadyDownload();
    }

    private DownloadType getWhenNotSupportRange(Response<Void> resp, String url) {
        long contentLength = Utils.contentLength(resp);
        if (mFileHelper.getFile(url).length() == contentLength) {
            return mBuilder.fileLength(contentLength).buildAlreadyDownload();
        } else {
            return mBuilder.url(url).fileLength(contentLength).lastModify(Utils.lastModify(resp)).buildNormalDownload();
        }
    }

    private void beforeDownload(String url, String saveName, String savePath) {
        if (TextUtils.isEmpty(mFileHelper.getDefaultPath())) {
            mFileHelper.setDefaultPath(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getPath());
        }
        if (mRetrofit == null) {
            mRetrofit = RetrofitProvider.getInstance();
        }
        if (mDownloadApi == null) {
            mDownloadApi = mRetrofit.create(DownloadApi.class);
            mFileHelper.setDownloadApi(mDownloadApi);
        }

        mFileHelper.addDownloadRecord(url, saveName, savePath);
    }
}
