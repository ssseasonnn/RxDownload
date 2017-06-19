package zlc.season.rxdownload2;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import zlc.season.rxdownload2.entity.DownloadBean;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadRecord;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownload2.entity.MultiMission;
import zlc.season.rxdownload2.entity.SingleMission;
import zlc.season.rxdownload2.function.DownloadHelper;
import zlc.season.rxdownload2.function.DownloadService;
import zlc.season.rxdownload2.function.Utils;

import static zlc.season.rxdownload2.function.Utils.log;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:46
 * RxDownload
 */
public class RxDownload {
    private static final Object object = new Object();
    @SuppressLint("StaticFieldLeak")
    private volatile static RxDownload instance;
    private volatile static boolean bound = false;

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

    private int maxDownloadNumber = 5;

    private Context context;
    private Semaphore semaphore;

    private DownloadService downloadService;
    private DownloadHelper downloadHelper;

    private RxDownload(Context context) {
        this.context = context.getApplicationContext();
        downloadHelper = new DownloadHelper(context);
        semaphore = new Semaphore(1);
    }

    /**
     * Return RxDownload Instance
     *
     * @param context context
     * @return RxDownload
     */
    public static RxDownload getInstance(Context context) {
        if (instance == null) {
            synchronized (RxDownload.class) {
                if (instance == null) {
                    instance = new RxDownload(context);
                }
            }
        }
        return instance;
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
        return downloadHelper.getFiles(url);
    }

    /**
     * get Files by saveName and savePath.
     *
     * @param saveName saveName
     * @param savePath savePath
     * @return Files
     */
    public File[] getRealFiles(String saveName, String savePath) {
        return Utils.getFiles(saveName, savePath);
    }

    /**
     * set default save path.
     *
     * @param savePath default save path.
     * @return instance.
     */
    public RxDownload defaultSavePath(String savePath) {
        downloadHelper.setDefaultSavePath(savePath);
        return this;
    }

    /**
     * If you have own Retrofit client, set it.
     *
     * @param retrofit retrofit client
     * @return instance.
     */
    public RxDownload retrofit(Retrofit retrofit) {
        downloadHelper.setRetrofit(retrofit);
        return this;
    }

    /**
     * set max thread to download file.
     *
     * @param max max threads
     * @return instance
     */
    public RxDownload maxThread(int max) {
        downloadHelper.setMaxThreads(max);
        return this;
    }

    /**
     * set max retry count when download failed
     *
     * @param max max retry count
     * @return instance
     */
    public RxDownload maxRetryCount(int max) {
        downloadHelper.setMaxRetryCount(max);
        return this;
    }

    /**
     * set max download number when service download
     *
     * @param max max download number
     * @return instance
     */
    public RxDownload maxDownloadNumber(int max) {
        this.maxDownloadNumber = max;
        return this;
    }

    /**
     * Receive the url download event.
     * <p>
     * Will receive the following event:
     * {@link DownloadFlag#NORMAL}、{@link DownloadFlag#WAITING}、
     * {@link DownloadFlag#STARTED}、{@link DownloadFlag#PAUSED}、
     * {@link DownloadFlag#COMPLETED}、{@link DownloadFlag#FAILED};
     * <p>
     * Every event has {@link DownloadStatus}, you can get it and display it on the interface.
     *
     * @param url url
     * @return DownloadEvent
     */
    public Observable<DownloadEvent> receiveDownloadStatus(final String url) {
        return createGeneralObservable(null)
                .flatMap(new Function<Object, ObservableSource<DownloadEvent>>() {
                    @Override
                    public ObservableSource<DownloadEvent> apply(Object o) throws Exception {
                        return downloadService.receiveDownloadEvent(url).toObservable();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Read all the download record from the database.
     *
     * @return Observable<List<DownloadRecord>>
     */
    public Observable<List<DownloadRecord>> getTotalDownloadRecords() {
        return downloadHelper.readAllRecords();
    }

    /**
     * Read single download record with url.
     * If record contain, return correct record, else return empty record.
     *
     * @param url download url
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadRecord> getDownloadRecord(String url) {
        return downloadHelper.readRecord(url);
    }

    /**
     * Pause download.
     * <p>
     * Pause a download.
     *
     * @param url url
     */
    public Observable<?> pauseServiceDownload(final String url) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                downloadService.pauseDownload(url);
            }
        }).observeOn(AndroidSchedulers.mainThread());

    }

    /**
     * Delete download.
     * <p>
     * Delete a download.
     *
     * @param url        url
     * @param deleteFile whether delete file
     */
    public Observable<?> deleteServiceDownload(final String url, final boolean deleteFile) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                downloadService.deleteDownload(url, deleteFile);
            }
        }).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Start all mission. Not include multi mission.
     *
     * @return Observable
     */
    public Observable<?> startAll() {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() throws InterruptedException {
                downloadService.startAll();
            }
        }).observeOn(AndroidSchedulers.mainThread());

    }

    /**
     * Pause all mission. Not include multi mission.
     *
     * @return Observable
     */
    public Observable<?> pauseAll() {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                downloadService.pauseAll();
            }
        }).observeOn(AndroidSchedulers.mainThread());

    }


    /**
     * Start all mission which associate with missionId
     *
     * @param missionId missionId
     * @return Observable
     */
    public Observable<?> startAll(final String missionId) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() throws InterruptedException {
                downloadService.startAll(missionId);
            }
        }).observeOn(AndroidSchedulers.mainThread());

    }

    public Observable<?> pauseAll(final String missionId) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                downloadService.pauseAll(missionId);
            }
        }).observeOn(AndroidSchedulers.mainThread());

    }

    /**
     * Delete all mission which associate with missionId.
     *
     * @param missionId  missionId
     * @param deleteFile deleteFile ?
     * @return Observable
     */
    public Observable<?> deleteAll(final String missionId, final boolean deleteFile) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() {
                downloadService.deleteAll(missionId, deleteFile);
            }
        }).observeOn(AndroidSchedulers.mainThread());

    }

    /**
     * Normal download.
     * <p>
     * Will save the download records in the database.
     * <p>
     * Un subscribe will pause download.
     *
     * @param url Url
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadStatus> download(String url) {
        return download(url, null);
    }

    /**
     * Normal download with assigned Name.
     *
     * @param url      url
     * @param saveName SaveName
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadStatus> download(String url, String saveName) {
        return download(url, saveName, null);
    }

    /**
     * Normal download with assigned name and path.
     *
     * @param url      url
     * @param saveName SaveName
     * @param savePath SavePath
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadStatus> download(String url, String saveName, String savePath) {
        return download(new DownloadBean.Builder(url).setSaveName(saveName)
                .setSavePath(savePath).build());
    }

    /**
     * Normal download.
     * <p>
     * You can construct a DownloadBean to save extra data to the database.
     *
     * @param downloadBean download bean.
     * @return Observable<DownloadStatus>
     */
    public Observable<DownloadStatus> download(DownloadBean downloadBean) {
        return downloadHelper.downloadDispatcher(downloadBean);
    }

    /**
     * Normal download for Transformer.
     *
     * @param url        url
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(String url) {
        return transform(url, null);
    }

    /**
     * Normal download for Transformer.
     *
     * @param url        url
     * @param saveName   saveName
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(
            String url, String saveName) {
        return transform(url, saveName, null);
    }

    /**
     * Normal download for Transformer.
     *
     * @param url        url
     * @param saveName   saveName
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(
            String url, String saveName, String savePath) {

        return transform(new DownloadBean.Builder(url)
                .setSaveName(saveName).setSavePath(savePath).build());
    }

    /**
     * Normal download version of the Transformer.
     *
     * @param downloadBean download bean
     * @param <Upstream>   Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, DownloadStatus> transform(
            final DownloadBean downloadBean) {
        return new ObservableTransformer<Upstream, DownloadStatus>() {
            @Override
            public ObservableSource<DownloadStatus> apply(Observable<Upstream> upstream) {
                return upstream.flatMap(new Function<Upstream, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(Upstream upstream) throws Exception {
                        return download(downloadBean);
                    }
                });
            }
        };
    }

    /**
     * Using Service to download single url.
     * <p>
     * Will save the download records in the database.
     * <p>
     * Un subscribe will not pause download.
     * <p>
     * If you want receive download status, see {@link #receiveDownloadStatus(String)}
     * <p>
     * If you want pause download, see {@link #pauseServiceDownload(String)}
     * <p>
     * If you want get record from database, see {@link #getDownloadRecord(String)}
     *
     * @param url url
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceDownload(String url) {
        return serviceDownload(url, "");
    }

    /**
     * Using Service to download.
     *
     * @param url      url
     * @param saveName saveName
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceDownload(String url, String saveName) {
        return serviceDownload(url, saveName, null);
    }

    /**
     * Using Service to download.
     *
     * @param url      url
     * @param saveName saveName
     * @param savePath savePath
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceDownload(String url, String saveName, String savePath) {
        return serviceDownload(new DownloadBean.Builder(url)
                .setSaveName(saveName).setSavePath(savePath).build());
    }

    /**
     * Using Service to download.
     *
     * @param bean download bean
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceDownload(final DownloadBean bean) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() throws InterruptedException {
                downloadService.addDownloadMission(new SingleMission(RxDownload.this, bean));
            }
        }).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Service download version of the Transformer.
     *
     * @param url        url
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformService(String url) {
        return transformService(url, null);
    }

    /**
     * Service download version of the Transformer.
     *
     * @param url        url
     * @param saveName   saveName
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformService(String url,
                                                                               String saveName) {
        return transformService(url, saveName, null);
    }

    /**
     * Service download version of the Transformer.
     *
     * @param url        url
     * @param saveName   saveName
     * @param savePath   savePath
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformService(
            String url, String saveName, String savePath) {

        return transformService(new DownloadBean.Builder(url)
                .setSaveName(saveName).setSavePath(savePath).build());
    }

    /**
     * Service download version of the Transformer.
     *
     * @param bean       download bean
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformService(final DownloadBean bean) {
        return new ObservableTransformer<Upstream, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Upstream> upstream) {
                return upstream.flatMap(new Function<Upstream, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Upstream upstream) throws Exception {
                        return serviceDownload(bean);
                    }
                });
            }
        };
    }

    /**
     * Using Service to download multi urls.
     *
     * @param missionId missionId
     * @param urls      urls
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceMultiDownload(String missionId, String... urls) {
        return serviceMultiDownload(missionId, Arrays.asList(urls));
    }

    /**
     * Using Service to download multi urls.
     *
     * @param missionId missionId
     * @param urls      List urls
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceMultiDownload(String missionId, List<String> urls) {
        List<DownloadBean> list = new ArrayList<>();
        for (String each : urls) {
            list.add(new DownloadBean.Builder(each).build());
        }
        return serviceMultiDownload(list, missionId);
    }

    /**
     * Using Service to download multi urls.
     *
     * @param beans     download beans
     * @param missionId missionId
     * @return Observable<DownloadStatus>
     */
    public Observable<?> serviceMultiDownload(final List<DownloadBean> beans, final String missionId) {
        return createGeneralObservable(new GeneralObservableCallback() {
            @Override
            public void call() throws InterruptedException {
                downloadService.addDownloadMission(new MultiMission(RxDownload.this, missionId, beans));
            }
        }).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Service multi download version of the Transformer.
     *
     * @param missionId  missionId
     * @param urls       multi download urls
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformMulti(
            String missionId, String... urls) {
        return transformMulti(missionId, Arrays.asList(urls));
    }

    /**
     * Service multi download version of the Transformer.
     *
     * @param missionId  missionId
     * @param urls       multi download urls
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformMulti(
            String missionId, List<String> urls) {
        List<DownloadBean> list = new ArrayList<>();
        for (String each : urls) {
            list.add(new DownloadBean.Builder(each).build());
        }
        return transformMulti(list, missionId);
    }

    /**
     * Service multi download version of the Transformer.
     *
     * @param beans      multi download bean
     * @param missionId  missionId
     * @param <Upstream> Upstream
     * @return Transformer
     */
    public <Upstream> ObservableTransformer<Upstream, Object> transformMulti(
            final List<DownloadBean> beans, final String missionId) {
        return new ObservableTransformer<Upstream, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Upstream> upstream) {
                return upstream.flatMap(new Function<Upstream, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Upstream upstream) throws Exception {
                        return serviceMultiDownload(beans, missionId);
                    }
                });
            }
        };
    }

    /**
     * return general observable
     *
     * @param callback Called when observable created.
     * @return Observable
     */
    private Observable<?> createGeneralObservable(final GeneralObservableCallback callback) {
        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(final ObservableEmitter<Object> emitter) throws Exception {
                if (!bound) {
                    semaphore.acquire();
                    if (!bound) {
                        startBindServiceAndDo(new ServiceConnectedCallback() {
                            @Override
                            public void call() {
                                doCall(callback, emitter);
                                semaphore.release();
                            }
                        });
                    } else {
                        doCall(callback, emitter);
                        semaphore.release();
                    }
                } else {
                    doCall(callback, emitter);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    private void doCall(GeneralObservableCallback callback, ObservableEmitter<Object> emitter) {
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

    /**
     * start and bind service.
     *
     * @param callback Called when service connected.
     */
    private void startBindServiceAndDo(final ServiceConnectedCallback callback) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(DownloadService.INTENT_KEY, maxDownloadNumber);
        context.startService(intent);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                DownloadService.DownloadBinder downloadBinder
                        = (DownloadService.DownloadBinder) binder;
                downloadService = downloadBinder.getService();
                context.unbindService(this);
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
