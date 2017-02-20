package zlc.season.rxdownload2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Retrofit;
import zlc.season.rxdownload2.db.DataBaseHelper;
import zlc.season.rxdownload2.entity.DownloadBean;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadMission;
import zlc.season.rxdownload2.entity.DownloadRecord;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownload2.function.DownloadHelper;
import zlc.season.rxdownload2.function.DownloadService;
import zlc.season.rxdownload2.function.Utils;

import static zlc.season.rxdownload2.function.Constant.CONTEXT_NULL_HINT;
import static zlc.season.rxdownload2.function.Utils.log;

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
                    log("Thread interrupted");
                } else if (throwable instanceof InterruptedIOException) {
                    log("Io interrupted");
                } else if (throwable instanceof SocketException) {
                    log("Socket error");
                }
            }
        });
    }

    private DownloadHelper mDownloadHelper;
    private Context mContext;
    private int MAX_DOWNLOAD_NUMBER = 5;

    private RxDownload(Context context) {
        this.mContext = context;
        mDownloadHelper = new DownloadHelper(context);
    }

    public static RxDownload getInstance(Context context) {
        return new RxDownload(context);
    }

    /**
     * get Files by url. May be NULL if this url record not exists.
     * File[] {DownloadFile, TempFile, LastModifyFile}
     *
     * @param url url
     * @return Files
     */
    @Nullable
    public File[] getRealFiles(String url) {
        return mDownloadHelper.getFiles(url);
    }

    /**
     * get Files by saveName and savePath.
     *
     * @param saveName saveName
     * @param savePath savePath
     * @return
     */
    public File[] getRealFiles(String saveName, String savePath) {
        return Utils.getFiles(saveName, savePath);
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
        return createGeneralObservable(null)
                .flatMap(new Function<Object,
                        ObservableSource<DownloadEvent>>() {
                    @Override
                    public ObservableSource<DownloadEvent> apply(Object o) throws Exception {
                        return mDownloadService.receiveDownloadEvent(url).toObservable();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Read all the download record from the database.
     * 从数据库中读取所有的下载记录
     *
     * @return Observable<List<DownloadRecord>>
     */
    public Observable<List<DownloadRecord>> getTotalDownloadRecords() {
        if (mContext == null) {
            return Observable.error(new Throwable(CONTEXT_NULL_HINT));
        }
        DataBaseHelper dataBaseHelper = DataBaseHelper
                .getSingleton(mContext.getApplicationContext());
        return dataBaseHelper.readAllRecords();
    }

    public DownloadRecord test() {

        DataBaseHelper dataBaseHelper = DataBaseHelper
                .getSingleton(mContext.getApplicationContext());
        return dataBaseHelper.readSingleRecord("http://downali.game.uc.cn/s/1/9/20170103112151d02a45_MY-1.110.0_uc_platform2.apk");
    }

    public boolean test1() {

        DataBaseHelper dataBaseHelper = DataBaseHelper
                .getSingleton(mContext.getApplicationContext());
        return dataBaseHelper.recordNotExists("http://dldir1.qq.com/weixin/android/weixin6330android920.apk");
    }

    /**
     * Read single download record with url.
     * If record contain, return correct record, else return empty record.
     * <p>
     * 从数据库中读取下载地址为url的下载记录, 如果数据库中存在该记录，则正常返回.
     * 如果不存在该记录，则返回一个空的DownloadRecord(url = null, saveName = null.)
     *
     * @param url download url
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadRecord> getDownloadRecord(String url) {
        if (mContext == null) {
            return Observable.error(new Throwable(CONTEXT_NULL_HINT));
        }
        DataBaseHelper dataBaseHelper = DataBaseHelper
                .getSingleton(mContext.getApplicationContext());
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
//                mDownloadService.cancelDownload(url);
            }
        });
    }

    /**
     * 删除Service中下载地址为url的下载任务.
     * <p>
     * 同时从数据库中删除该下载记录.
     * <p>
     * when deleteFiles is true, the downloaded file will be deleted.
     *
     * @param url        download url
     * @param deleteFile whether delete  file
     */
    public Observable<?> deleteServiceDownload(final String url, final boolean deleteFile) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                mDownloadService.deleteDownload(url, deleteFile);
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
     * @param savePath download file SavePath. If NULL, using default save path {@code
     *                 /storage/emulated/0/Download/}
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceDownload(@NonNull final String url,
                                         @Nullable final String saveName,
                                         @Nullable final String savePath) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() throws InterruptedException {
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
     * @param savePath download file SavePath. If NULL, using default save path {@code
     *                 /storage/emulated/0/Download/}
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadStatus> download(@NonNull final String url, @Nullable final String saveName,
                                               @Nullable final String savePath) {

        return download(new DownloadBean.Builder(url)
                .setSaveName(saveName)
                .setSavePath(savePath)
                .build());
    }

    public Observable<DownloadStatus> download(String url) {
        return download(url, null);
    }

    public Observable<DownloadStatus> download(String url, String saveName) {
        return download(url, saveName, null);
    }

    public Observable<DownloadStatus> download(DownloadBean downloadBean) {
        return mDownloadHelper.downloadDispatcher(downloadBean);
    }

    /**
     * Normal download version of the Transformer.
     * <p>
     * Provide RxJava Compose operator use.
     *
     * @param url        download file Url
     * @param saveName   download file SaveName
     * @param savePath   download file SavePath. If NULL, using default save path {@code
     *                   /storage/emulated/0/Download/}
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(
            @NonNull final String url,
            @Nullable final String saveName,
            @Nullable final String savePath) {
        return new ObservableTransformer<Upstream, DownloadStatus>() {
            @Override
            public ObservableSource<DownloadStatus> apply(
                    io.reactivex.Observable<Upstream> upstream) {
                return upstream.flatMap(new Function<Upstream, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(Upstream upstream)
                            throws Exception {
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
     * @param savePath   download file SavePath. If NULL, using default save path {@code
     *                   /storage/emulated/0/Download/}
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformService(
            @NonNull final String url,
            @Nullable final String saveName,
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

    private void addDownloadTask(@NonNull String url, @Nullable String saveName,
                                 @Nullable String savePath) throws InterruptedException {
        mDownloadService.addDownloadMission(
                new DownloadMission(
                        new DownloadBean.Builder(url)
                                .setSaveName(saveName)
                                .setSavePath(savePath)
                                .build(),
                        RxDownload.this));
    }

    private Observable<?> createGeneralObservable(final GeneralObservableCallback callback) {
        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(final ObservableEmitter<Object> emitter) throws Exception {
                if (!bound) {
                    startBindServiceAndDo(new ServiceConnectedCallback() {
                        @Override
                        public void call() {
                            if (callback != null) {
                                try {
                                    callback.call();
                                } catch (Exception e) {
                                    emitter.onError(e);
                                }
                            }
                            emitter.onNext(object);
                            emitter.onComplete();
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.call();
                    }
                    emitter.onNext(object);
                    emitter.onComplete();
                }
            }
        });
    }

    private void startBindServiceAndDo(final ServiceConnectedCallback callback) {
        if (mContext == null) {
            throw new IllegalArgumentException(CONTEXT_NULL_HINT);
        }
        Intent intent = new Intent(mContext, DownloadService.class);
        intent.putExtra(DownloadService.INTENT_KEY, MAX_DOWNLOAD_NUMBER);
        mContext.startService(intent);
        mContext.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                DownloadService.DownloadBinder downloadBinder
                        = (DownloadService.DownloadBinder) binder;
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
        void call() throws Exception;
    }

    private interface ServiceConnectedCallback {
        void call();
    }
}
