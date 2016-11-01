package zlc.season.rxdownload;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.CompositeException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;


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
    private static final int IF_MODIFIED_SINCE = 4;
    private static final int IF_NONE_MATCH = 5;

    private static final String TEST_RANGE_SUPPORT = "bytes=0-";
    private static final String SUFFIX = ".tmp";

    private final int EACH_RECORD_SIZE = 16; //long + long = 8 + 8
    private int MAX_RETRY_COUNT = 3;
    private int RECORD_FILE_TOTAL_SIZE;
    private int MAX_THREADS = 3;
    private boolean mLastModifySupport = false;
    private boolean mEtagSupport = false;
    //|********************|
    //|*****Record File****|
    //|********************|
    //| start   |     end  |
    //|********************|
    //|  0L     |     7L   |
    //|  8L     |     15L  |
    //|  16L    |     31L  |
    //|********************|
    private DownloadApi mDownloadApi;
    private Retrofit mRetrofit;
    private String mDefaultPath;

    private RxDownload() {

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
        return this;
    }

    public RxDownload maxRetryCount(int max) {
        MAX_RETRY_COUNT = max;
        return this;
    }

    public void testHead() {
        beforeDownload();
        mDownloadApi.getHeaders(TEST_RANGE_SUPPORT, "http://dldir1.qq.com/weixin/android/weixin6327android880.apk")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response<Void>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w(TAG, e);
                    }

                    @Override
                    public void onNext(Response<Void> voidResponse) {
                        Log.d(TAG, "voidResponse.headers():" + voidResponse.headers());
                    }
                });
    }

    /**
     * 开始下载
     *
     * @param url      下载文件的Url
     * @param saveName 下载文件的保存名称, null使用默认的名称(从url或响应头中读取文件名)
     * @param savePath 下载文件的保存路径, null使用默认的路径,默认保存在/sdcard/Download/目录下
     * @return Observable
     */
    public Observable<DownloadStatus> download(@NonNull final String url, @NonNull final String saveName,
                                               @Nullable final String savePath) {
        beforeDownload();
        try {
            return downloadDispatcher(url, saveName, savePath);
        } catch (IOException e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }

    public String lastModifyToGMTStr(long lastModify) {
        Date d = new Date(lastModify);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(d);
    }

    public long GMTStrToLastModify(String GMT) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        Date date = sdf.parse(GMT);
        return date.getTime();
    }

    private Observable<DownloadStatus> downloadDispatcher(@NonNull final String url, @NonNull String saveName,
                                                          @Nullable String savePath) throws IOException {
        final String filePath = getFileSavePath(savePath) + File.separator + saveName;
        final File file = new File(filePath);
        Observable<Integer> result;
        if (file.exists()) {
            result = createFileExistsObservable(url, filePath, file);
        } else {
            result = createFileNotExistsObservable(url);
        }
        return result.flatMap(new Func1<Integer, Observable<DownloadStatus>>() {
            @Override
            public Observable<DownloadStatus> call(Integer integer) {
                switch (integer) {
                    case NORMAL_DOWNLOAD:
                        Log.i(TAG, "Normal download start!");
                        return startNormalDownload(url, filePath);
                    case MULTI_THREAD_DOWNLOAD:
                        Log.i(TAG, "Multi thread download start!");
                        //                        prepareDownload(filePath, contentLength);
                        try {
                            return startMultiThreadDownload(filePath, url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case IF_MODIFIED_SINCE:

                        return null;
                    case IF_NONE_MATCH:

                        return null;
                    case CONTINUE_DOWNLOAD:
                        Log.i(TAG, "Continue download start!");
                        try {
                            return startMultiThreadDownload(filePath, url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    case FILE_ALREADY_DOWNLOADED:
                        Log.i(TAG, "Already downloaded!");
                        //                        return Observable.just(new DownloadStatus(contentLength,
                        // contentLength));
                    default:
                        Log.i(TAG, "unknown error!");
                        return Observable.error(new Throwable("unknown error!"));
                }
                //                return null;
            }
        });
    }

    private Observable<Integer> createFileNotExistsObservable(@NonNull String url) {
        return mDownloadApi.getHeaders(TEST_RANGE_SUPPORT, url)
                .map(new Func1<Response<Void>, Integer>() {
                    @Override
                    public Integer call(Response<Void> response) {
                        long contentLength = HttpHeaders.contentLength(response.headers());
                        String contentRange = response.headers().get("Content-Range");
                        boolean notSupportRange = TextUtils.isEmpty(contentRange) || contentLength == -1;
                        if (notSupportRange) {
                            return NORMAL_DOWNLOAD;
                        } else {
                            return MULTI_THREAD_DOWNLOAD;
                        }
                    }
                });
    }

    private Observable<Integer> createFileExistsObservable(@NonNull String url, final String filePath,
                                                           final File file) {
        return mDownloadApi.getHeadersWithLastModify(file.lastModified(), url)
                .map(new Func1<Response<Void>, Integer>() {
                    @Override
                    public Integer call(Response<Void> response) {

                        long contentLength = HttpHeaders.contentLength(response.headers());
                        String contentRange = response.headers().get("Content-Range");
                        boolean notSupportRange = TextUtils.isEmpty(contentRange) || contentLength == -1;
                        if (response.code() == 304) {
                            //server file does't change
                            if (notSupportRange) {
                                if (file.length() == contentLength) {
                                    return FILE_ALREADY_DOWNLOADED;
                                } else {
                                    return NORMAL_DOWNLOAD;
                                }
                            } else {
                                String recordPath = filePath + SUFFIX;
                                File recordFile = new File(recordPath);
                                if (!recordFile.exists()) {
                                    return MULTI_THREAD_DOWNLOAD;
                                }
                                return CONTINUE_DOWNLOAD;
                            }
                        } else {
                            //server file has changed
                            if (notSupportRange) {
                                return NORMAL_DOWNLOAD;
                            } else {
                                return MULTI_THREAD_DOWNLOAD;
                            }
                        }
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
        RECORD_FILE_TOTAL_SIZE = MAX_THREADS * EACH_RECORD_SIZE;
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
                        return saveNormalFile(savePath, response);
                    }
                }).onBackpressureLatest().retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return retry(integer, throwable);
                    }
                });
    }

    private Observable<DownloadStatus> saveNormalFile(final String savePath, final Response<ResponseBody>
            response) {
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                specificSaveNormalFile(subscriber, savePath, response);
            }
        });
    }

    private void specificSaveNormalFile(Subscriber<? super DownloadStatus> subscriber,
                                        String savePath, Response<ResponseBody> response) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            try {
                int readLen;
                int downloadSize = 0;
                byte[] buffer = new byte[8192];

                File file = new File(savePath);
                //                file.setLastModified()
                DownloadStatus status = new DownloadStatus();

                inputStream = response.body().byteStream();
                outputStream = new FileOutputStream(file);

                long contentLength = response.body().contentLength();
                boolean isChunked = !TextUtils.isEmpty(response.headers().get("Transfer-Encoding"));
                if (isChunked || contentLength == -1) {
                    status.isChunked = true;
                }
                status.setTotalSize(contentLength);

                while ((readLen = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, readLen);
                    downloadSize += readLen;
                    status.setDownloadSize(downloadSize);
                    subscriber.onNext(status);
                }
                outputStream.flush(); // This is important!!!
                subscriber.onCompleted();
                Log.i(TAG, "Normal download completed!");
            } finally {
                closeUtils(inputStream);
                closeUtils(outputStream);
                closeUtils(response.body());
            }
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }

    /**
     * 多线程+断点续传
     *
     * @param filePath filePath
     * @param url      url
     * @return Observable
     * @throws IOException
     */
    private Observable<DownloadStatus> startMultiThreadDownload(final String filePath, String url) throws IOException {
        DownloadRange range = getDownloadRange(filePath);
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
                        return saveRangeFile(start, end, i, filePath, response.body());
                    }
                }).onBackpressureLatest().retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return retry(integer, throwable);
                    }
                });
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
    private Observable<DownloadStatus> saveRangeFile(final long start, final long end, final int i,
                                                     final String filePath, final ResponseBody response) {
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                specificSaveRangeFile(subscriber, i, start, end, filePath, response);
            }
        });
    }

    private void specificSaveRangeFile(Subscriber<? super DownloadStatus> subscriber, int i,
                                       long start, long end, String filePath, ResponseBody response) {
        RandomAccessFile record = null;
        FileChannel recordChannel = null;

        RandomAccessFile save = null;
        FileChannel saveChannel = null;

        InputStream inStream = null;
        try {
            try {
                Log.i(TAG, Thread.currentThread().getName() + " start download from " + start + " to " + end + "!");
                int readLen;
                byte[] buffer = new byte[8192];
                DownloadStatus status = new DownloadStatus();

                record = new RandomAccessFile(filePath + SUFFIX, "rws");
                recordChannel = record.getChannel();
                MappedByteBuffer recordBuffer = recordChannel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
                long totalSize = recordBuffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
                status.setTotalSize(totalSize);

                save = new RandomAccessFile(filePath, "rws");
                saveChannel = save.getChannel();
                MappedByteBuffer saveBuffer = saveChannel.map(READ_WRITE, start, end - start + 1);

                inStream = response.byteStream();
                while ((readLen = inStream.read(buffer)) != -1) {
                    saveBuffer.put(buffer, 0, readLen);
                    recordBuffer.putLong(i * EACH_RECORD_SIZE, recordBuffer.getLong(i * EACH_RECORD_SIZE) + readLen);

                    status.setDownloadSize(totalSize - getResidue(recordBuffer));
                    subscriber.onNext(status);
                }
                Log.i(TAG, Thread.currentThread().getName() + " complete download! Download size is " +
                        response.contentLength() + " bytes");
                subscriber.onCompleted();
            } finally {
                closeUtils(record);
                closeUtils(recordChannel);
                closeUtils(save);
                closeUtils(saveChannel);
                closeUtils(inStream);
                closeUtils(response);
            }
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }

    /**
     * 还剩多少字节没有下载
     *
     * @param recordBuffer buffer
     * @return 剩余的字节
     */
    private long getResidue(MappedByteBuffer recordBuffer) {
        long residue = 0;
        for (int j = 0; j < MAX_THREADS; j++) {
            long startTemp = recordBuffer.getLong(j * EACH_RECORD_SIZE);
            long endTemp = recordBuffer.getLong(j * EACH_RECORD_SIZE + 8);
            long temp = endTemp - startTemp + 1;
            residue += temp;
        }
        return residue;
    }

    private DownloadRange getDownloadRange(String filePath) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(filePath + SUFFIX, "rws");
            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
            long[] startByteArray = new long[MAX_THREADS];
            long[] endByteArray = new long[MAX_THREADS];
            for (int i = 0; i < MAX_THREADS; i++) {
                startByteArray[i] = buffer.getLong();
                endByteArray[i] = buffer.getLong();
            }
            return new DownloadRange(startByteArray, endByteArray);
        } finally {
            closeUtils(channel);
            closeUtils(record);
        }
    }

    private void prepareDownload(String path, long contentLength) throws IOException {
        RandomAccessFile file = null;
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            file = new RandomAccessFile(path, "rws");
            file.setLength(contentLength);//设置下载文件的长度

            record = new RandomAccessFile(path + SUFFIX, "rws");
            record.setLength(RECORD_FILE_TOTAL_SIZE); //设置指针记录文件的大小

            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);

            long start;
            long end;
            int eachSize = (int) (contentLength / MAX_THREADS);
            for (int i = 0; i < MAX_THREADS; i++) {
                if (i == MAX_THREADS - 1) {
                    start = i * eachSize;
                    end = contentLength - 1;
                } else {
                    start = i * eachSize;
                    end = (i + 1) * eachSize - 1;
                }
                buffer.putLong(start);
                buffer.putLong(end);
            }
        } finally {
            closeUtils(channel);
            closeUtils(record);
            closeUtils(file);
        }
    }

    private String getFileSavePath(String savePath) throws IOException {
        if (!TextUtils.isEmpty(savePath)) {
            File file = new File(savePath);
            boolean create = file.createNewFile();
            if (create) {
                Log.i(TAG, "create file save path success");
            } else {
                Log.i(TAG, "file save path already exists");
            }
            if (file.isDirectory()) {
                return savePath;
            } else {
                throw new IllegalArgumentException("Are you kidding me? You give me a file path, But I need a " +
                        "directory path");
            }
        }
        return mDefaultPath;
    }


    private boolean recordFileDamaged(String recordFilePath, long contentLength) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(recordFilePath, "rws");
            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
            long recordTotalSize = buffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
            return recordTotalSize != contentLength;
        } finally {
            closeUtils(channel);
            closeUtils(record);
        }
    }

    private boolean downloadNotComplete(String recordFilePath) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(recordFilePath, "rws");
            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
            long startByte;
            long endByte;
            for (int i = 0; i < MAX_THREADS; i++) {
                startByte = buffer.getLong();
                endByte = buffer.getLong();
                if (startByte <= endByte) {
                    return true;
                }
            }
            return false;
        } finally {
            closeUtils(channel);
            closeUtils(record);
        }
    }

    private void closeUtils(Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }
}
