package zlc.season.rxdownload;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Response;
import retrofit2.Retrofit;
import zlc.season.rxdownload.db.DataBaseHelper;
import zlc.season.rxdownload.entity.DownloadEvent;
import zlc.season.rxdownload.entity.DownloadMission;
import zlc.season.rxdownload.entity.DownloadRecord;
import zlc.season.rxdownload.entity.DownloadStatus;
import zlc.season.rxdownload.entity.DownloadType;
import zlc.season.rxdownload.entity.DownloadTypeFactory;
import zlc.season.rxdownload.function.DownloadHelper;
import zlc.season.rxdownload.function.DownloadService;
import zlc.season.rxdownload.function.Utils;

import static zlc.season.rxdownload.function.DownloadHelper.TEST_RANGE_SUPPORT;
import static zlc.season.rxdownload.function.FileHelper.TAG;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:46
 * RxDownload
 */
public class RxDownload {
    private static DownloadService mDownloadService;
    private static boolean bound = false;
    private static Object object = new Object();

    static {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                if (throwable instanceof InterruptedException) {
                    Log.d(TAG, "Thread interrupted");
                } else if (throwable instanceof InterruptedIOException) {
                    Log.d(TAG, "Io interrupted");
                } else if (throwable instanceof SocketException) {
                    Log.d(TAG, "Socket error");
                }
            }
        });
    }

    private DownloadHelper mDownloadHelper;
    private DownloadTypeFactory mFactory;
    private Context mContext;
    private boolean mAutoInstall;
    private int MAX_DOWNLOAD_NUMBER = 5;

    private RxDownload() {
        mDownloadHelper = new DownloadHelper();
        mFactory = new DownloadTypeFactory(mDownloadHelper);
    }

    public static RxDownload getInstance() {
        return new RxDownload();
    }

    /**
     * 普通下载时不需要context, 使用Service下载时需要context;
     *
     * @param context context
     * @return RxDownload
     */
    public RxDownload context(Context context) {
        this.mContext = context;
        return this;
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

    public RxDownload maxDownloadNumber(int max) {
        this.MAX_DOWNLOAD_NUMBER = max;
        return this;
    }

    public RxDownload autoInstall(boolean flag) {
        this.mAutoInstall = flag;
        return this;
    }

    /**
     * Receive the download address for the url download event and download status.
     * 接收下载地址为url的下载事件和下载状态.
     * <p>
     * Note that only receive the download address for the URL.
     * 注意只接收下载地址为url的事件和状态.
     *
     * @param url download url
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadEvent> receiveDownloadStatus(final String url) {
        return Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(final SingleEmitter<Object> e) throws Exception {
                if (!bound) {
                    startBindServiceAndDo(new ServiceConnectedCallback() {
                        @Override
                        public void call() {
                            e.onSuccess(object);
                        }
                    });
                } else {
                    e.onSuccess(object);
                }
            }
        }).flatMapObservable(new Function<Object, ObservableSource<? extends DownloadEvent>>() {
            @Override
            public ObservableSource<? extends DownloadEvent> apply(Object o) throws Exception {
                return mDownloadService.getProcessor(url).toObservable();
            }
        }).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Read all the download record from the database
     * 从数据库中读取所有的下载记录
     *
     * @return Observable<List<DownloadRecord>>
     */
    public Observable<List<DownloadRecord>> getTotalDownloadRecords() {
        if (mContext == null) {
            return Observable.error(new Throwable("Context is NULL! You should call " +
                    "#RxDownload.context(Context context)# first!"));
        }
        DataBaseHelper dataBaseHelper = DataBaseHelper.getSingleton(mContext);
        return dataBaseHelper.readAllRecords();
    }

    /**
     * Read single download record with url.
     * 从数据库中读取下载地址为url的下载记录
     *
     * @param url download url
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadRecord> getDownloadRecord(String url) {
        if (mContext == null) {
            return Observable.error(new Throwable("Context is NULL! You should call " +
                    "#RxDownload.context(Context context)# first!"));
        }
        DataBaseHelper dataBaseHelper = DataBaseHelper.getSingleton(mContext);
        return dataBaseHelper.readRecord(url);
    }

    /**
     * Suspended download address for the url download task in Service.
     * 暂停Service中下载地址为url的下载任务.
     * <p>
     * Book the download records in the tag database are paused.
     * 同时标记数据库中的下载记录为暂停状态.
     *
     * @param url download url
     */
    public Observable<?> pauseServiceDownload(final String url) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                mDownloadService.pauseDownload(url);
            }
        });
    }

    /**
     * 取消Service中下载地址为url的下载任务.
     * <p>
     * 同时标记数据库中的下载记录为取消状态.
     * 不会删除已经下载的文件.
     *
     * @param url download url
     */
    public Observable<?> cancelServiceDownload(final String url) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                mDownloadService.cancelDownload(url);
            }
        });
    }

    /**
     * 删除Service中下载地址为url的下载任务.
     * <p>
     * 同时从数据库中删除该下载记录.
     * 不会删除已经下载的文件.
     *
     * @param url download url
     */
    public Observable<?> deleteServiceDownload(final String url) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                mDownloadService.deleteDownload(url);
            }
        });
    }

    /**
     * Using Service to download. Just download, can't receive download status.
     * 使用Service下载. 仅仅开始下载, 不会接收下载进度.
     * <p>
     * Un subscribe will not pause download.
     * 取消订阅不会停止下载.
     * <p>
     * If you want receive download status, see {@link #receiveDownloadStatus(String)}
     * <p>
     * If you want pause download, see {@link #pauseServiceDownload(String)}
     * <p>
     * Also save the download records in the database, if you want get record from database,
     * see  {@link #getDownloadRecord(String)}
     *
     * @param url      download file Url
     * @param saveName download file SaveName
     * @param savePath download file SavePath. If NULL, using default save path {@code /storage/emulated/0/Download/}
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceDownload(@NonNull final String url,
                                         @NonNull final String saveName,
                                         @Nullable final String savePath) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                addDownloadTask(url, saveName, savePath);
            }
        });
    }

    /**
     * Normal download.
     * <p>
     * Un subscribe will  pause download.
     * <p>
     * Do not save the download records in the database.
     *
     * @param url      download file Url
     * @param saveName download file SaveName
     * @param savePath download file SavePath. If NULL, using default save path {@code /storage/emulated/0/Download/}
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadStatus> download(@NonNull final String url,
                                               @NonNull final String saveName,
                                               @Nullable final String savePath) {
        return downloadDispatcher(url, saveName, savePath);
    }

    /**
     * Normal download version of the Transformer.
     * <p>
     * Provide RxJava Compose operator use.
     *
     * @param url        download file Url
     * @param saveName   download file SaveName
     * @param savePath   download file SavePath. If NULL, using default save path {@code /storage/emulated/0/Download/}
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(@NonNull final String url,
                                                                                @NonNull final String saveName,
                                                                                @Nullable final String savePath) {

        return new ObservableTransformer<Upstream, DownloadStatus>() {
            @Override
            public ObservableSource<DownloadStatus> apply(io.reactivex.Observable<Upstream> upstream) {
                return upstream.flatMap(new Function<Upstream, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(Upstream upstream) throws Exception {
                        return download(url, saveName, savePath);
                    }
                });
            }
        };
    }

    /**
     * Service download without status version of the Transformer.
     * <p>
     * Provide RxJava Compose operator use.
     *
     * @param url        download file Url
     * @param saveName   download file SaveName
     * @param savePath   download file SavePath. If NULL, using default save path {@code /storage/emulated/0/Download/}
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformService(@NonNull final String url,
                                                                               @NonNull final String saveName,
                                                                               @Nullable final String savePath) {
        return new ObservableTransformer<Upstream, Object>() {

            @Override
            public ObservableSource<Object> apply(Observable<Upstream> upstream) {
                return upstream.flatMap(new Function<Upstream, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Upstream upstream) throws Exception {
                        return serviceDownload(url, saveName, savePath);
                    }
                });
            }
        };
    }

    public String[] getRealFileSavePaths(String savePath) {
        return mDownloadHelper.getFileSavePaths(savePath);
    }

    public File[] getRealFiles(String saveName, String savePath) {
        String[] filePaths = mDownloadHelper.getRealFilePaths(saveName, savePath);
        return new File[]{new File(filePaths[0]), new File(filePaths[1]), new File(filePaths[2])};
    }

    private Observable<?> createGeneralObservable(final GeneralObservableCallback callback) {
        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(final ObservableEmitter<Object> emitter) throws Exception {
                if (!bound) {
                    startBindServiceAndDo(new ServiceConnectedCallback() {
                        @Override
                        public void call() {
                            callback.call();
                            emitter.onNext(object);
                            emitter.onComplete();
                        }
                    });
                } else {
                    callback.call();
                    emitter.onNext(object);
                    emitter.onComplete();
                }
            }
        });
    }

    private void addDownloadTask(@NonNull String url, @NonNull String saveName,
                                 @Nullable String savePath) {
        mDownloadService.addDownloadMission(
                new DownloadMission.Builder()
                        .setRxDownload(RxDownload.this)
                        .setUrl(url)
                        .setSaveName(saveName)
                        .setSavePath(savePath)
                        .build());
    }

    private Observable<DownloadStatus> downloadDispatcher(final String url,
                                                          final String saveName,
                                                          final String savePath) {
        if (mDownloadHelper.isRecordExists(url)) {
            return Observable.error(new Throwable("This url download task already exists, so do nothing."));
        }
        try {
            mDownloadHelper.addDownloadRecord(url, saveName, savePath);
        } catch (IOException e) {
            return Observable.error(e);
        }
        return getDownloadType(url)
                .flatMap(new Function<DownloadType, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(DownloadType downloadType) throws Exception {
                        downloadType.prepareDownload();
                        return downloadType.startDownload();
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        //等待1.5秒,以确保文件写入到磁盘中.
                        Thread.sleep(1500);
                        if (mAutoInstall) {
                            if (mContext == null) {
                                throw new IllegalStateException("Context is NULL! You should call " +
                                        "#RxDownload.context(Context context)# first!");
                            }
                            Utils.installApk(mContext, getRealFiles(saveName, savePath)[0]);
                        }
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (throwable instanceof CompositeException) {
                            Log.w(TAG, throwable.getMessage());
                        } else {
                            Log.w(TAG, throwable);
                        }
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        mDownloadHelper.deleteDownloadRecord(url);
                    }
                });
    }

    private Observable<DownloadType> getDownloadType(String url) {
        if (mDownloadHelper.downloadFileExists(url)) {
            try {
                return getWhenFileExists(url);
            } catch (IOException e) {
                return getWhenFileNotExists(url);
            }
        } else {
            return getWhenFileNotExists(url);
        }
    }

    private Observable<DownloadType> getWhenFileNotExists(final String url) {
        return mDownloadHelper.getDownloadApi()
                .getHttpHeader(TEST_RANGE_SUPPORT, url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> response) throws Exception {
                        if (Utils.notSupportRange(response)) {
                            return mFactory.url(url)
                                    .fileLength(Utils.contentLength(response))
                                    .lastModify(Utils.lastModify(response))
                                    .buildNormalDownload();
                        } else {
                            return mFactory.url(url)
                                    .lastModify(Utils.lastModify(response))
                                    .fileLength(Utils.contentLength(response))
                                    .buildMultiDownload();
                        }
                    }
                })
                .retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(Integer integer, Throwable throwable) throws Exception {
                        return mDownloadHelper.retry(integer, throwable);
                    }
                });

    }

    private Observable<DownloadType> getWhenFileExists(final String url) throws IOException {
        return mDownloadHelper.getDownloadApi()
                .getHttpHeaderWithIfRange(TEST_RANGE_SUPPORT, mDownloadHelper.getLastModify(url), url)
                .map(new Function<Response<Void>, DownloadType>() {
                    @Override
                    public DownloadType apply(Response<Void> resp) throws Exception {
                        if (Utils.serverFileNotChange(resp)) {
                            return getWhenServerFileNotChange(resp, url);
                        } else if (Utils.serverFileChanged(resp)) {
                            return getWhenServerFileChanged(resp, url);
                        } else {
                            throw new RuntimeException("unknown error");
                        }
                    }
                })
                .retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(Integer integer, Throwable throwable) throws Exception {
                        return mDownloadHelper.retry(integer, throwable);
                    }
                });
    }

    private DownloadType getWhenServerFileChanged(Response<Void> resp, String url) {
        if (Utils.notSupportRange(resp)) {
            return mFactory.url(url)
                    .fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp))
                    .buildNormalDownload();
        } else {
            return mFactory.url(url)
                    .fileLength(Utils.contentLength(resp))
                    .lastModify(Utils.lastModify(resp))
                    .buildMultiDownload();
        }
    }

    private DownloadType getWhenServerFileNotChange(Response<Void> resp, String url) {
        if (Utils.notSupportRange(resp)) {
            return getWhenNotSupportRange(resp, url);
        } else {
            return getWhenSupportRange(resp, url);
        }
    }

    private DownloadType getWhenSupportRange(Response<Void> resp, String url) {
        long contentLength = Utils.contentLength(resp);
        try {
            if (mDownloadHelper.needReDownload(url, contentLength)) {
                return mFactory.url(url)
                        .fileLength(contentLength)
                        .lastModify(Utils.lastModify(resp))
                        .buildMultiDownload();
            }
            if (mDownloadHelper.downloadNotComplete(url)) {
                return mFactory.url(url)
                        .fileLength(contentLength)
                        .lastModify(Utils.lastModify(resp))
                        .buildContinueDownload();
            }
        } catch (IOException e) {
            Log.w(TAG, "download record file may be damaged,so we will re download");
            return mFactory.url(url)
                    .fileLength(contentLength)
                    .lastModify(Utils.lastModify(resp))
                    .buildMultiDownload();
        }
        return mFactory.fileLength(contentLength).buildAlreadyDownload();
    }

    private DownloadType getWhenNotSupportRange(Response<Void> resp, String url) {
        long contentLength = Utils.contentLength(resp);
        if (mDownloadHelper.downloadNotComplete(url, contentLength)) {
            return mFactory.url(url)
                    .fileLength(contentLength)
                    .lastModify(Utils.lastModify(resp))
                    .buildNormalDownload();
        } else {
            return mFactory.fileLength(contentLength).buildAlreadyDownload();
        }
    }

    private void startBindServiceAndDo(final ServiceConnectedCallback callback) {
        if (mContext == null) {
            throw new IllegalArgumentException("Context is NULL! You should call " +
                    "#RxDownload.context(Context context)# first!");
        }
        Intent intent = new Intent(mContext, DownloadService.class);
        intent.putExtra(DownloadService.INTENT_KEY, MAX_DOWNLOAD_NUMBER);
        mContext.startService(intent);
        mContext.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                DownloadService.DownloadBinder downloadBinder = (DownloadService.DownloadBinder) binder;
                mDownloadService = downloadBinder.getService();
                mContext.unbindService(this);
                bound = true;
                callback.call();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                //注意!!这个方法只会在系统杀掉Service时才会调用!!
                bound = false;
            }
        }, Context.BIND_AUTO_CREATE);
    }

    private interface GeneralObservableCallback {
        void call();
    }

    private interface ServiceConnectedCallback {
        void call();
    }
}
