package zlc.season.rxdownload;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import retrofit2.Retrofit;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import zlc.season.rxdownload.db.DataBaseHelper;
import zlc.season.rxdownload.entity.DownloadEvent;
import zlc.season.rxdownload.entity.DownloadMission;
import zlc.season.rxdownload.entity.DownloadRecord;
import zlc.season.rxdownload.entity.DownloadStatus;
import zlc.season.rxdownload.function.DownloadHelper;
import zlc.season.rxdownload.function.DownloadService;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:46
 * RxDownload
 */
public class RxDownload {
    private static DownloadService mDownloadService;
    private static boolean bound = false;

    private DownloadHelper mDownloadHelper;

    private Context mContext;

    private boolean mAutoInstall;
    private int MAX_DOWNLOAD_NUMBER = 5;

    private RxDownload() {
        mDownloadHelper = new DownloadHelper();
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
     * get Files.
     * File[] {DownloadFile, TempFile, LastModifyFile}
     *
     * @param saveName saveName
     * @param savePath savePath
     * @return Files
     */
    public File[] getRealFiles(String saveName, String savePath) {
        String[] filePaths = mDownloadHelper.getRealFilePaths(saveName, savePath);
        return new File[]{new File(filePaths[0]), new File(filePaths[1]), new File(filePaths[2])};
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
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(final Subscriber<? super Object> subscriber) {
                if (!bound) {
                    startBindServiceAndDo(new ServiceConnectedCallback() {
                        @Override
                        public void call() {
                            subscriber.onNext(null);
                        }
                    });
                } else {
                    subscriber.onNext(null);
                }
            }
        }).flatMap(new Func1<Object, Observable<DownloadEvent>>() {
            @Override
            public Observable<DownloadEvent> call(Object o) {
                return mDownloadService.getSubject(RxDownload.this, url).asObservable().onBackpressureLatest();
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
        return Observable.just(null).doOnSubscribe(new Action0() {
            @Override
            public void call() {
                if (!bound) {
                    startBindServiceAndDo(new ServiceConnectedCallback() {
                        @Override
                        public void call() {
                            mDownloadService.pauseDownload(url);
                        }
                    });
                } else {
                    mDownloadService.pauseDownload(url);
                }
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
        return Observable.just(null).doOnSubscribe(new Action0() {
            @Override
            public void call() {
                if (!bound) {
                    startBindServiceAndDo(new ServiceConnectedCallback() {
                        @Override
                        public void call() {
                            mDownloadService.cancelDownload(url);
                        }
                    });
                } else {
                    mDownloadService.cancelDownload(url);
                }
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
        return Observable.just(null).doOnSubscribe(new Action0() {
            @Override
            public void call() {
                if (!bound) {
                    startBindServiceAndDo(new ServiceConnectedCallback() {
                        @Override
                        public void call() {
                            mDownloadService.deleteDownload(url);
                        }
                    });
                } else {
                    mDownloadService.deleteDownload(url);
                }
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
    public Observable<Object> serviceDownload(@NonNull final String url, @NonNull final String saveName,
                                              @Nullable final String savePath) {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(final Subscriber<? super Object> subscriber) {
                if (!bound) {
                    startBindServiceAndDo(new ServiceConnectedCallback() {
                        @Override
                        public void call() {
                            try {
                                addDownloadTask(url, saveName, savePath);
                                subscriber.onNext(null);
                                subscriber.onCompleted();
                            } catch (IOException e) {
                                subscriber.onError(e);
                            }
                        }
                    });
                } else {
                    try {
                        addDownloadTask(url, saveName, savePath);
                        subscriber.onNext(null);
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
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
    public Observable<DownloadStatus> download(@NonNull final String url, @NonNull final String saveName,
                                               @Nullable final String savePath) {
        return mDownloadHelper.downloadDispatcher(url, saveName, savePath, mContext, mAutoInstall);
    }

    /**
     * Normal download version of the Transformer.
     * <p>
     * Provide RxJava Compose operator use.
     *
     * @param url      download file Url
     * @param saveName download file SaveName
     * @param savePath download file SavePath. If NULL, using default save path {@code /storage/emulated/0/Download/}
     * @param <T>      T
     * @return Transformer
     */
    public <T> Observable.Transformer<T, DownloadStatus> transform(@NonNull final String url,
                                                                   @NonNull final String saveName,
                                                                   @Nullable final String savePath) {
        return new Observable.Transformer<T, DownloadStatus>() {
            @Override
            public Observable<DownloadStatus> call(Observable<T> observable) {
                return observable.flatMap(new Func1<T, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(T t) {
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
     * @param url      download file Url
     * @param saveName download file SaveName
     * @param savePath download file SavePath. If NULL, using default save path {@code /storage/emulated/0/Download/}
     * @param <T>      T
     * @return Transformer
     */
    public <T> Observable.Transformer<T, Object> transformService(@NonNull final String url,
                                                                  @NonNull final String saveName,
                                                                  @Nullable final String savePath) {
        return new Observable.Transformer<T, Object>() {
            @Override
            public Observable<Object> call(Observable<T> observable) {
                return observable.flatMap(new Func1<T, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(T t) {
                        return serviceDownload(url, saveName, savePath);
                    }
                });
            }
        };
    }

    private void addDownloadTask(@NonNull String url, @NonNull String saveName,
                                 @Nullable String savePath) throws IOException {
        mDownloadService.addDownloadMission(
                new DownloadMission.Builder()
                        .setRxDownload(RxDownload.this)
                        .setUrl(url)
                        .setSaveName(saveName)
                        .setSavePath(savePath)
                        .build());
    }

    private void startBindServiceAndDo(final ServiceConnectedCallback callback) {
        if (mContext == null) {
            throw new RuntimeException("Context is NULL! You should call " +
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

    private interface ServiceConnectedCallback {
        void call();
    }
}
