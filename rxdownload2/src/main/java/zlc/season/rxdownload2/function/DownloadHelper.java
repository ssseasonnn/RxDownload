package zlc.season.rxdownload2.function;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import retrofit2.Response;
import retrofit2.Retrofit;
import zlc.season.rxdownload2.db.DataBaseHelper;
import zlc.season.rxdownload2.entity.DownloadBean;
import zlc.season.rxdownload2.entity.DownloadRecord;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownload2.entity.DownloadType;
import zlc.season.rxdownload2.entity.TemporaryRecord;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static zlc.season.rxdownload2.function.Constant.DOWNLOAD_URL_EXISTS;
import static zlc.season.rxdownload2.function.Constant.REQUEST_RETRY_HINT;
import static zlc.season.rxdownload2.function.Constant.TEST_RANGE_SUPPORT;
import static zlc.season.rxdownload2.function.Constant.URL_ILLEGAL;
import static zlc.season.rxdownload2.function.Utils.formatStr;
import static zlc.season.rxdownload2.function.Utils.log;
import static zlc.season.rxdownload2.function.Utils.retry;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:39
 * Download helper
 */
public class DownloadHelper {
    private int maxRetryCount = 3;
    private int maxThreads = 3;

    private String defaultSavePath;
    private DownloadApi downloadApi;

    private DataBaseHelper dataBaseHelper;
    private TemporaryRecordTable recordTable;

    public DownloadHelper(Context context) {
        downloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
        defaultSavePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
        recordTable = new TemporaryRecordTable();
        dataBaseHelper = DataBaseHelper.getSingleton(context.getApplicationContext());
    }

    public void setRetrofit(Retrofit retrofit) {
        downloadApi = retrofit.create(DownloadApi.class);
    }

    public void setDefaultSavePath(String defaultSavePath) {
        this.defaultSavePath = defaultSavePath;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * return Files
     *
     * @param url url
     * @return Files = {file,tempFile,lmfFile}
     */
    @Nullable
    public File[] getFiles(String url) {
        DownloadRecord record = dataBaseHelper.readSingleRecord(url);
        if (record == null) {
            return null;
        } else {
            return Utils.getFiles(record.getSaveName(), record.getSavePath());
        }
    }

    /**
     * dispatch download
     *
     * @param bean download bean
     * @return DownloadStatus
     */
    public Observable<DownloadStatus> downloadDispatcher(final DownloadBean bean) {
        return Observable.just(1)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        addTempRecord(bean);
                    }
                })
                .flatMap(new Function<Integer, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Integer integer) throws Exception {
                        return getDownloadType(bean.getUrl());
                    }
                })
                .flatMap(new Function<DownloadType, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(DownloadType type) throws Exception {
                        return download(type);
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        logError(throwable);
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        recordTable.delete(bean.getUrl());
                    }
                });
    }

    private ObservableSource<DownloadStatus> download(DownloadType downloadType)
            throws IOException, ParseException {
        downloadType.prepareDownload();
        return downloadType.startDownload();
    }

    private void logError(Throwable throwable) {
        if (throwable instanceof CompositeException) {
            CompositeException realException = (CompositeException) throwable;
            List<Throwable> exceptions = realException.getExceptions();
            for (Throwable each : exceptions) {
                log(each);
            }
        } else {
            log(throwable);
        }
    }

    /**
     * Add a temporary record to the record recordTable.
     *
     * @param bean download bean
     */
    private void addTempRecord(DownloadBean bean) {
        if (recordTable.contain(bean.getUrl())) {
            throw new IllegalArgumentException(formatStr(DOWNLOAD_URL_EXISTS, bean.getUrl()));
        }
        recordTable.add(bean.getUrl(), new TemporaryRecord(bean));
    }

    /**
     * get download type.
     *
     * @param url url
     * @return download type
     */
    private Observable<DownloadType> getDownloadType(final String url) {
        return Observable.just(1)
                .flatMap(new Function<Integer, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Integer integer)
                            throws Exception {
                        return checkUrl(url);
                    }
                })
                .flatMap(new Function<Object, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Object o) throws Exception {
                        return checkRange(url);
                    }
                })
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        recordTable.init(url, maxThreads, maxRetryCount, defaultSavePath,
                                downloadApi, dataBaseHelper);
                    }
                })
                .flatMap(new Function<Object, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Object o) throws Exception {
                        return recordTable.fileExists(url) ? existsType(url) : nonExistsType(url);
                    }
                });
    }

    /**
     * Gets the download type of file non-existence.
     *
     * @param url file url
     * @return Download Type
     */
    private Observable<DownloadType> nonExistsType(final String url) {
        return Observable.just(1)
                .flatMap(new Function<Integer, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Integer integer)
                            throws Exception {
                        return Observable.just(recordTable.generateNonExistsType(url));
                    }
                });
    }

    /**
     * Gets the download type of file existence.
     *
     * @param url file url
     * @return Download Type
     */
    private Observable<DownloadType> existsType(final String url) {
        return Observable.just(1)
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        return recordTable.readLastModify(url);
                    }
                })
                .flatMap(new Function<String, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(String s) throws Exception {
                        return checkFile(url, s);
                    }
                })
                .flatMap(new Function<Object, ObservableSource<DownloadType>>() {
                    @Override
                    public ObservableSource<DownloadType> apply(Object o)
                            throws Exception {
                        return Observable.just(recordTable.generateFileExistsType(url));
                    }
                });
    }

    /**
     * check url
     *
     * @param url url
     * @return empty
     */
    private ObservableSource<Object> checkUrl(final String url) {
        return downloadApi.check(url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(Response<Void> response) throws Exception {
                        if (!response.isSuccessful()) {
                            throw new IllegalArgumentException(formatStr(URL_ILLEGAL, url));
                        } else {
                            recordTable.saveFileInfo(url, response);
                        }
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(Response<Void> response) throws Exception {
                        return new Object();
                    }
                })
                .compose(retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    /**
     * http checkRangeByHead request,checkRange need info.
     *
     * @param url url
     * @return empty Observable
     */
    private ObservableSource<Object> checkRange(final String url) {
        return downloadApi.checkRangeByHead(TEST_RANGE_SUPPORT, url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(Response<Void> response) throws Exception {
                        recordTable.saveRangeInfo(url, response);
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(Response<Void> response) throws Exception {
                        return new Object();
                    }
                })
                .compose(retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    /**
     * http checkRangeByHead request,checkRange need info, check whether if server file has changed.
     *
     * @param url url
     * @return empty Observable
     */
    private ObservableSource<Object> checkFile(final String url, String lastModify) {
        return downloadApi.checkFileByHead(lastModify, url)
                .doOnNext(new Consumer<Response<Void>>() {
                    @Override
                    public void accept(Response<Void> response) throws Exception {
                        recordTable.saveFileState(url, response);
                    }
                })
                .map(new Function<Response<Void>, Object>() {
                    @Override
                    public Object apply(Response<Void> response) throws Exception {
                        return new Object();
                    }
                })
                .compose(retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    public Observable<List<DownloadRecord>> readAllRecords() {
        return dataBaseHelper.readAllRecords();
    }

    public Observable<DownloadRecord> readRecord(String url) {
        return dataBaseHelper.readRecord(url);
    }
}
