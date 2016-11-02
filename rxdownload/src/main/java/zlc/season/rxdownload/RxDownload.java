package zlc.season.rxdownload;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:46
 * FIXME
 */
public class RxDownload {
    private static final String TAG = "RxDownload";

    private static final int NORMAL_DOWNLOAD = 0;
    private static final int MULTI_THREAD_DOWNLOAD = 1;
    private static final int CONTINUE_DOWNLOAD = 2;
    private static final int FILE_ALREADY_DOWNLOADED = 3;

    private static final String TEST_RANGE_SUPPORT = "bytes=0-";

    private int MAX_RETRY_COUNT = 3;
    private int MAX_THREADS = 3;

    private DownloadApi mDownloadApi;
    private Retrofit mRetrofit;
    private String mDefaultPath;

    private FileHelper mFileHelper;

    private RxDownload() {
        mFileHelper = new FileHelper();
    }

    public static RxDownload getInstance() {
        return new RxDownload();
    }

    public RxDownload defaultSavePath(String savePath) {
        this.mDefaultPath = savePath;
        return this;
    }

    public RxDownload retrofit(Retrofit retrofit) {
        this.mRetrofit = retrofit;
        return this;
    }

    public RxDownload maxThread(int max) {
        MAX_THREADS = max;
        mFileHelper.setMaxThreads(max);
        return this;
    }

    public RxDownload maxRetryCount(int max) {
        MAX_RETRY_COUNT = max;
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
        final String filePath = getFileSavePath(savePath) + File.separator + saveName;
        return downloadDispatcher(url, filePath);
    }

    private Observable<DownloadStatus> downloadDispatcher(final String url, final String filePath) {
        Observable<MapResult> result;
        try {
            result = createObservable(url, filePath);
        } catch (IOException e) {
            Log.w(TAG, e);
            return Observable.error(new Throwable("create download observable failed", e));
        }
        return result
                .flatMap(new Func1<MapResult, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(MapResult result) {
                        switch (result.type) {
                            case NORMAL_DOWNLOAD:
                                Log.i(TAG, "Normal download start!");
                                try {
                                    mFileHelper.prepareNormalDownload(filePath, result.fileLength);
                                    mFileHelper.writeLastModify(filePath, result.lastModify);
                                    return startNormalDownload(url, filePath);
                                } catch (IOException | ParseException e) {
                                    return Observable.error(new Throwable("start normal download failed", e));
                                }

                            case MULTI_THREAD_DOWNLOAD:
                                Log.i(TAG, "Multi thread download start!");
                                try {
                                    mFileHelper.prepareMultiThreadDownload(filePath, result.fileLength);
                                    mFileHelper.writeLastModify(filePath, result.lastModify);
                                    return startMultiThreadDownload(url, filePath);
                                } catch (IOException | ParseException e) {
                                    return Observable.error(new Throwable("start multi thread download failed", e));
                                }

                            case CONTINUE_DOWNLOAD:
                                Log.i(TAG, "Continue download start!");
                                try {
                                    return startMultiThreadDownload(url, filePath);
                                } catch (IOException e) {
                                    return Observable.error(new Throwable("continue multi thread download failed", e));
                                }
                            case FILE_ALREADY_DOWNLOADED:
                                Log.i(TAG, "Already downloaded!");
                                return Observable.just(new DownloadStatus(result.fileLength, result.fileLength));
                            default:
                                Log.i(TAG, "unknown error!");
                                return Observable.error(new Throwable("unknown error!"));
                        }
                    }
                }).doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.w(TAG, throwable);
                    }
                });
    }

    private Observable<MapResult> createObservable(String url, String filePath) throws IOException {
        final File file = new File(filePath);
        if (file.exists()) {
            return createFileExistsObservable(url, filePath, file);
        } else {
            return createFileNotExistsObservable(url);
        }
    }

    private Observable<MapResult> createFileNotExistsObservable(@NonNull String url) {
        return mDownloadApi.getHeaders(TEST_RANGE_SUPPORT, url)
                .map(new Func1<Response<Void>, MapResult>() {
                    @Override
                    public MapResult call(Response<Void> response) {
                        long contentLength = HttpHeaders.contentLength(response.headers());
                        String contentRange = response.headers().get("Content-Range");
                        boolean notSupportRange = TextUtils.isEmpty(contentRange) || contentLength == -1;
                        if (notSupportRange) {
                            return new MapResult(NORMAL_DOWNLOAD, contentLength);
                        } else {
                            MapResult result = new MapResult(MULTI_THREAD_DOWNLOAD, contentLength);
                            result.lastModify = response.headers().get("Last-Modified");
                            return result;
                        }
                    }
                });
    }

    private Observable<MapResult> createFileExistsObservable(String url, final String filePath,
                                                             final File file) throws IOException {
        return mDownloadApi.getHeadersWithIfRange(TEST_RANGE_SUPPORT, mFileHelper.getLastModify(filePath), url)
                .map(new Func1<Response<Void>, MapResult>() {
                    @Override
                    public MapResult call(Response<Void> response) {
                        long contentLength = HttpHeaders.contentLength(response.headers());
                        String contentRange = response.headers().get("Content-Range");
                        boolean notSupportRange = TextUtils.isEmpty(contentRange) || contentLength == -1;
                        if (response.code() == 206) { //server file no changed
                            return getMapResultWhen206(response, contentLength, notSupportRange, file, filePath);
                        } else {  //server file has changed, need re download
                            return getMapResultWhen200(response, contentLength, notSupportRange);
                        }
                    }
                });
    }

    @NonNull
    private MapResult getMapResultWhen200(Response<Void> response, long contentLength, boolean notSupportRange) {
        if (notSupportRange) {
            return new MapResult(NORMAL_DOWNLOAD, contentLength, Utils.lastModify(response));
        } else {
            return new MapResult(MULTI_THREAD_DOWNLOAD, contentLength, Utils.lastModify(response));
        }
    }

    @NonNull
    private MapResult getMapResultWhen206(Response<Void> response, long contentLength, boolean notSupportRange,
                                          File file, String filePath) {
        if (notSupportRange) {
            return getMapResultWhenNotRange(response, contentLength, file);
        } else {
            return getResultWhenRange(response, contentLength, filePath);
        }
    }

    @NonNull
    private MapResult getResultWhenRange(Response<Void> response, long contentLength, String filePath) {
        try {
            String recordPath = filePath + mFileHelper.getSuffix();
            File recordFile = new File(recordPath);
            if (!recordFile.exists()) {
                return new MapResult(MULTI_THREAD_DOWNLOAD, contentLength,
                        Utils.lastModify(response));
            }
            if (mFileHelper.recordFileDamaged(filePath, contentLength)) {
                return new MapResult(MULTI_THREAD_DOWNLOAD, contentLength,
                        Utils.lastModify(response));
            }
            if (mFileHelper.downloadNotComplete(filePath)) {
                return new MapResult(CONTINUE_DOWNLOAD, contentLength);
            }
        } catch (IOException e) {
            return new MapResult(MULTI_THREAD_DOWNLOAD, contentLength,
                    Utils.lastModify(response));
        }
        return new MapResult(FILE_ALREADY_DOWNLOADED, contentLength);
    }

    @NonNull
    private MapResult getMapResultWhenNotRange(Response<Void> response, long contentLength, File file) {
        if (file.length() == contentLength) {
            return new MapResult(FILE_ALREADY_DOWNLOADED, contentLength);
        } else {
            return new MapResult(NORMAL_DOWNLOAD, contentLength, Utils.lastModify(response));
        }
    }

    /**
     * 常规下载, 不采用多线程和断点续传
     *
     * @param url      url
     * @param savePath 下载文件保存路径
     * @return Observable
     */
    private Observable<DownloadStatus> startNormalDownload(final String url, final String savePath) {
        return mDownloadApi.download(null, url)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(final Response<ResponseBody> response) {
                        return normalSave(savePath, response);
                    }
                }).onBackpressureLatest().retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return retry(integer, throwable);
                    }
                });
    }

    private Observable<DownloadStatus> normalSave(final String savePath, final Response<ResponseBody> response) {
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                mFileHelper.saveNormalFile(subscriber, savePath, response);
            }
        });
    }

    /**
     * 多线程+断点续传
     *
     * @param url      url
     * @param filePath filePath
     * @return Observable
     */
    private Observable<DownloadStatus> startMultiThreadDownload(String url, final String filePath) throws IOException {
        DownloadRange range = mFileHelper.getDownloadRange(filePath);
        List<Observable<DownloadStatus>> tasks = new ArrayList<>();
        for (int i = 0; i < MAX_THREADS; i++) {
            if (range.start[i] <= range.end[i]) {
                tasks.add(rangeDownloadTask(range.start[i], range.end[i], i, url, filePath));
            }
        }
        return Observable.mergeDelayError(tasks);
    }

    /**
     * 分段下载的任务
     *
     * @param start    从start字节开始
     * @param end      到end字节结束
     * @param i        下载编号
     * @param url      下载地址
     * @param filePath 保存路径
     * @return Observable
     */
    private Observable<DownloadStatus> rangeDownloadTask(final long start, final long end, final int i,
                                                         final String url, final String filePath) {
        String range = "bytes=" + start + "-" + end;
        return mDownloadApi.download(range, url)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(Response<ResponseBody> response) {
                        return rangeSave(start, end, i, filePath, response.body());
                    }
                }).onBackpressureLatest().retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return retry(integer, throwable);
                    }
                });
    }

    /**
     * 保存断点下载的文件,以及下载进度
     *
     * @param start    从start开始
     * @param end      到end结束
     * @param i        下载编号
     * @param filePath 保存路径
     * @param response 响应值
     * @return Observable
     */
    private Observable<DownloadStatus> rangeSave(final long start, final long end, final int i,
                                                 final String filePath, final ResponseBody response) {
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                mFileHelper.saveRangeFile(subscriber, i, start, end, filePath, response);
            }
        });
    }

    private void beforeDownload() {
        if (TextUtils.isEmpty(mDefaultPath)) {
            mDefaultPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        }
        if (mRetrofit == null) {
            mRetrofit = RetrofitProvider.getInstance();
        }
        mDownloadApi = mRetrofit.create(DownloadApi.class);
    }

    @NonNull
    private Boolean retry(Integer integer, Throwable throwable) {
        if (throwable instanceof UnknownHostException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " no network, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof HttpException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " had non-2XX http error, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof SocketTimeoutException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " socket time out,retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof SocketException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, Thread.currentThread().getName() +
                        " a network or conversion error happened, retry to connect " + integer + " times");
                return true;
            }
            return false;
        } else if (throwable instanceof CompositeException) {
            Log.w(TAG, throwable.getMessage());
            return false;
        } else {
            Log.w(TAG, throwable);
            return false;
        }
    }

    private String getFileSavePath(String savePath) {
        if (!TextUtils.isEmpty(savePath)) {
            File file = new File(savePath);
            if (file.exists() && file.isDirectory()) {
                return savePath;
            } else {
                boolean flag = file.mkdir();
                if (flag) {
                    Log.i(TAG, "create file save path success");
                    return savePath;
                } else {
                    Log.i(TAG, "create file save path failed , now use default save path");
                }
            }
        }
        return mDefaultPath;
    }

    private class MapResult {
        Integer type;
        long fileLength;
        String lastModify;

        MapResult(Integer type, long fileLength) {
            this.type = type;
            this.fileLength = fileLength;
        }

        public MapResult(Integer type, long fileLength, String lastModify) {
            this.type = type;
            this.fileLength = fileLength;
            this.lastModify = lastModify;
        }
    }
}
