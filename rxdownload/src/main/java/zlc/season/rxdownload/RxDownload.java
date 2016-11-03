package zlc.season.rxdownload;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
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

    private FileHelper mHelper;
    private DownloadType.Builder mBuilder;

    private RxDownload() {
        mHelper = new FileHelper();
    }

    public static RxDownload getInstance() {
        return new RxDownload();
    }

    public RxDownload defaultSavePath(String savePath) {
        mHelper.setDefaultPath(savePath);
        return this;
    }

    public RxDownload retrofit(Retrofit retrofit) {
        this.mRetrofit = retrofit;
        return this;
    }

    public RxDownload maxThread(int max) {
        mHelper.setMaxThreads(max);
        return this;
    }

    public RxDownload maxRetryCount(int max) {
        mHelper.setMaxRetryCount(max);
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
        beforeDownload();
        return downloadDispatcher(url, mHelper.getFilePath(saveName, savePath));
    }


    private Observable<DownloadStatus> downloadDispatcher(final String url, final String filePath) {
        return getDownloadType(url, filePath)
                .flatMap(new Func1<DownloadType, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(DownloadType type) {
                        try {
                            type.prepareDownload();
                        } catch (IOException | ParseException e) {
                            Log.w(TAG, e);
                            return Observable.error(e);
                        }
                        try {
                            return type.startDownload();
                        } catch (IOException e) {
                            Log.w(TAG, e);
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

    private Observable<DownloadType> getDownloadType(String url, String filePath) {
        final File file = new File(filePath);
        if (file.exists()) {
            return getWhenFileExists(filePath, file.length(), url);
        } else {
            return getWhenFileNotExists(url, filePath);
        }
    }

    private Observable<DownloadType> getWhenFileNotExists(@NonNull final String url, final String filePath) {
        return mDownloadApi.getHttpHeader(TEST_RANGE_SUPPORT, url)
                .map(new Func1<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType call(Response<Void> response) {
                        if (Utils.notSupportRange(response)) {
                            return mBuilder.url(url).filePath(filePath).fileLength(Utils.contentLength(response))
                                    .lastModify(Utils.lastModify(response))
                                    .buildNormalDownload();
                        } else {
                            return mBuilder.url(url).lastModify(Utils.lastModify(response))
                                    .filePath(filePath).fileLength(Utils.contentLength(response))
                                    .buildMultiDownload();
                        }
                    }
                });
    }

    private Observable<DownloadType> getWhenFileExists(final String filePath, final long fileLength, final String url) {
        return mDownloadApi.getHttpHeaderWithIfRange(TEST_RANGE_SUPPORT, mHelper.getLastModify(filePath), url)
                .map(new Func1<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType call(Response<Void> resp) {
                        if (resp.code() == 206) {
                            //server file no changed
                            return getWhen206(resp, fileLength, filePath, url);
                        } else {
                            //server file has changed, need re download
                            return getWhen200(resp, filePath, url);
                        }
                    }
                });
    }

    @NonNull
    private DownloadType getWhen200(Response<Void> resp, String filePath, String url) {
        if (Utils.notSupportRange(resp)) {
            return mBuilder.url(url).filePath(filePath).fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp)).buildNormalDownload();
        } else {
            return mBuilder.url(url).filePath(filePath).fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp)).buildMultiDownload();
        }
    }

    @NonNull
    private DownloadType getWhen206(Response<Void> resp, long fileLength, String filePath, String url) {
        if (Utils.notSupportRange(resp)) {
            return getWhenNotSupportRange(resp, fileLength, filePath, url);
        } else {
            return getWhenSupportRange(resp, filePath, url);
        }
    }

    @NonNull
    private DownloadType getWhenSupportRange(Response<Void> resp, String filePath, String url) {
        long contentLength = Utils.contentLength(resp);
        try {
            if (mHelper.recordFileNotExists(filePath) || mHelper.recordFileDamaged(filePath, contentLength)) {
                return mBuilder.url(url).filePath(filePath).fileLength(contentLength)
                        .lastModify(Utils.lastModify(resp)).buildMultiDownload();
            }
            if (mHelper.downloadNotComplete(filePath)) {
                return mBuilder.url(url).filePath(filePath).fileLength(contentLength)
                        .lastModify(Utils.lastModify(resp)).buildContinueDownload();
            }
        } catch (IOException e) {
            Log.w(TAG, "download record file may be damaged,so we will re download");
            return mBuilder.url(url).filePath(filePath).fileLength(contentLength)
                    .lastModify(Utils.lastModify(resp)).buildMultiDownload();
        }
        return mBuilder.fileLength(contentLength).buildAlreadyDownload();
    }

    @NonNull
    private DownloadType getWhenNotSupportRange(Response<Void> resp, long fileLength, String filePath, String url) {
        long contentLength = Utils.contentLength(resp);
        if (fileLength == contentLength) {
            return mBuilder.fileLength(contentLength).buildAlreadyDownload();
        } else {
            return mBuilder.url(url).filePath(filePath).fileLength(contentLength)
                    .lastModify(Utils.lastModify(resp)).buildNormalDownload();
        }
    }


    private void beforeDownload() {
        if (TextUtils.isEmpty(mHelper.getDefaultPath())) {
            mHelper.setDefaultPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .getPath());
        }
        if (mRetrofit == null) {
            mRetrofit = RetrofitProvider.getInstance();
        }
        mDownloadApi = mRetrofit.create(DownloadApi.class);
        mBuilder = new DownloadType.Builder(mDownloadApi, mHelper);
    }
}
