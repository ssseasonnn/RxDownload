package zlc.season.rxdownload;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:46
 * FIXME
 */
public class RxDownload {

    private static final String TAG = "RxDownloader";

    private static final int NORMAL_DOWNLOAD = 0;
    private static final int RE_DOWNLOAD = 1;
    private static final int CONTINUE_DOWNLOAD = 2;
    private static final int FILE_ALREADY_DOWNLOADED = 3;

    private static final String RANGE_TEST = "bytes=0-";
    private static final String SUFFIX = ".tmp";

    private final int EACH_RECORD_SIZE = 16; //long + long = 8 + 8
    private int RECORD_FILE_TOTAL_SIZE;
    private int MAX_THREADS = 3;

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

    public Observable<String> combineTest() {
        final String[] s1 = new String[]{"1", "2", "3", "4", "5"};
        final String[] s2 = new String[]{"a", "b", "c", "d", "e"};
        Observable<String> test1 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                for (String each : s1) {
                    subscriber.onNext(each);
                    Log.d(TAG, "test1 send: " + each);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                subscriber.onCompleted();
                Log.d(TAG, "test1 send complete");
            }
        })
                .subscribeOn(Schedulers.io());

        Observable<String> test2 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                for (String each : s2) {
                    subscriber.onNext(each);
                    Log.d(TAG, "test2 send: " + each);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                subscriber.onCompleted();
                Log.d(TAG, "test2 send complete");
            }
        })
                .subscribeOn(Schedulers.io());

        Observable<String> result = Observable.combineLatest(test1, test2, new Func2<String, String, String>() {
            @Override
            public String call(String integer, String integer2) {
                return integer + " - " + integer2;
            }
        }).subscribeOn(Schedulers.io());
        return result;
    }

    public Observable<DownloadStatus> download(final String url, final String saveName, final String savePath) {
        beforeDownload();
        return mDownloadApi.download(RANGE_TEST, url)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(final Response<ResponseBody> response) {
                        try {
                            return createDownloadObservable(response, saveName, savePath, url);
                        } catch (IOException e) {
                            Log.w(TAG, e);
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    private void beforeDownload() {
        if (TextUtils.isEmpty(mDefaultPath)) {
            if (!isExternalStorageWritable()) {
                throw new IllegalStateException("SD Card 不可用");
            }
            mDefaultPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        }
        if (mRetrofit == null) {
            mRetrofit = RetrofitProvider.getInstance();
        }

        RECORD_FILE_TOTAL_SIZE = MAX_THREADS * EACH_RECORD_SIZE;

        mDownloadApi = mRetrofit.create(DownloadApi.class);
    }

    private Observable<DownloadStatus> createDownloadObservable(Response<ResponseBody> response, String saveName,
                                                                String savePath, String url) throws IOException {
        String fileName = getFileSaveName(saveName, url, response.headers());
        String filePath = getFileSavePath(savePath) + File.separator + fileName;

        final long contentLength = response.body().contentLength();
        Log.d(TAG, "contentLength:" + contentLength);

        int type = getDownloadType(response, filePath);
        switch (type) {
            case NORMAL_DOWNLOAD:
                return startNormalDownload(filePath, response.body());
            case RE_DOWNLOAD:
                /**
                 * prepare download
                 */
                prepareDownload(filePath, contentLength);
                closeUtils(response.body());
                return startMultiThreadDownload(filePath, url);
            case CONTINUE_DOWNLOAD:
                closeUtils(response.body());
                return startMultiThreadDownload(filePath, url);
            case FILE_ALREADY_DOWNLOADED:
                closeUtils(response.body());
                return Observable.just(new DownloadStatus(contentLength, contentLength));
            default:
                closeUtils(response.body());
                throw new RuntimeException("unknown error");
        }
    }

    /**
     * 常规下载, 不采用多线程和断点续传
     *
     * @param savePath 下载文件保存路径
     * @param response Response
     * @return Observable
     */
    private Observable<DownloadStatus> startNormalDownload(final String savePath, final ResponseBody response) {
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                try {
                    specificSaveNormalFile(subscriber, savePath, response);
                } catch (IOException e) {
                    Log.w(TAG, e);
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io()).sample(100, TimeUnit.MILLISECONDS)
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.w(TAG, throwable);
                    }
                });
        //                .buffer(2, TimeUnit.SECONDS)
        //                .map(new Func1<List<DownloadStatus>, DownloadStatus>() {
        //                    @Override
        //                    public DownloadStatus call(List<DownloadStatus> downloadStatuses) {
        //                        if (downloadStatuses.size() > 0) {
        //                            return downloadStatuses.get(downloadStatuses.size() - 1);
        //                        }
        //                        return new DownloadStatus(0, 0);
        //                    }
        //                })
        //                .flatMap(new Func1<List<DownloadStatus>, Observable<DownloadStatus>>() {
        //                    @Override
        //                    public Observable<DownloadStatus> call(List<DownloadStatus> list) {
        //                        DownloadStatus result = new DownloadStatus();
        //                        for (DownloadStatus each : list) {
        //                            result.downloadSize += each.downloadSize;
        //                            result.totalSize = each.totalSize;
        //                        }
        //                        Log.d(TAG, "buffer");
        //                        Log.d(TAG, "result. " + result.downloadSize + " - " + result.totalSize);
        //                        return Observable.just(result);
        //                    }
        //                })

    }

    private void specificSaveNormalFile(Subscriber<? super DownloadStatus> subscriber, String savePath,
                                        ResponseBody response) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            int readLen;
            byte[] buffer = new byte[8192];
            File file = new File(savePath);
            DownloadStatus status = new DownloadStatus();

            inputStream = response.byteStream();
            outputStream = new BufferedOutputStream(new FileOutputStream(file));

            long contentLength = response.contentLength();
            if (contentLength == -1) {
                status.isChuncked = true;
            }
            status.totalSize = contentLength;

            while ((readLen = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readLen);
                status.downloadSize += readLen;
                subscriber.onNext(status);
            }
            outputStream.flush(); // This is important!!!
            subscriber.onCompleted();
        } finally {
            closeUtils(inputStream);
            closeUtils(outputStream);
            closeUtils(response);
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
    private Observable<DownloadStatus> startMultiThreadDownload(String filePath, String url) throws
            IOException {
        DownloadRange range = getDownloadRange(filePath);
        List<Observable<DownloadStatus>> tasks = new ArrayList<>();
        for (int i = 0; i < MAX_THREADS; i++) {
            if (range.start[i] <= range.end[i]) {
                tasks.add(rangeDownloadTask(range.start[i], range.end[i], i, url, filePath));
            }
        }
        return Observable.combineLatest(tasks, new FuncN<DownloadStatus>() {
            @Override
            public DownloadStatus call(Object... args) {
                DownloadStatus total = new DownloadStatus();
                for (Object each : args) {
                    DownloadStatus temp = (DownloadStatus) each;
                    total.totalSize += temp.totalSize;
                    total.downloadSize += temp.downloadSize;
                }
                return total;
            }
        }).doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.w(TAG, throwable);
            }
        });
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
                }).sample(100, TimeUnit.MILLISECONDS);
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
                try {
                    specificSaveRangeFile(subscriber, start, end, filePath, i, response);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.w(TAG, e);
                    subscriber.onError(e);
                }
            }
        });
    }

    private void specificSaveRangeFile(Subscriber<? super DownloadStatus> subscriber,
                                       long start, long end, String filePath, int i,
                                       ResponseBody response) throws IOException {
        RandomAccessFile record = null;
        FileChannel recordChannel = null;

        RandomAccessFile save = null;
        FileChannel saveChannel = null;

        InputStream inStream = null;
        try {
            Log.i(TAG, "Range download start! " + Thread.currentThread().getName() + " start from " +
                    start + " to " + end);

            byte[] buffer = new byte[4096];
            int readLen;
            DownloadStatus status = new DownloadStatus();
            status.totalSize = response.contentLength();

            record = new RandomAccessFile(filePath + SUFFIX, "rwd");
            recordChannel = record.getChannel();
            MappedByteBuffer recordBuffer = recordChannel.map(READ_WRITE, i * EACH_RECORD_SIZE, EACH_RECORD_SIZE);

            save = new RandomAccessFile(filePath, "rwd");
            saveChannel = save.getChannel();
            MappedByteBuffer saveBuffer = saveChannel.map(READ_WRITE, start, end - start + 1);

            inStream = new BufferedInputStream(response.byteStream());
            while ((readLen = inStream.read(buffer)) != -1) {
                saveBuffer.put(buffer, 0, readLen);
                recordBuffer.putLong(0, recordBuffer.getLong(0) + readLen);
                status.downloadSize += readLen;
                subscriber.onNext(status);
            }
            Log.i(TAG, "Range download complete! " + Thread.currentThread().getName() + " download " +
                    status.downloadSize + " size");
            subscriber.onCompleted();
        } finally {
            closeUtils(record);
            closeUtils(recordChannel);
            closeUtils(save);
            closeUtils(saveChannel);
            closeUtils(inStream);
            closeUtils(response);
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private DownloadRange getDownloadRange(String filePath) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(filePath + SUFFIX, "rwd");
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
            file = new RandomAccessFile(path, "rwd");
            file.setLength(contentLength);//设置下载文件的长度

            record = new RandomAccessFile(path + SUFFIX, "rwd");
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

    private String getFileSaveName(String saveName, String url, Headers headers) {
        if (!TextUtils.isEmpty(saveName)) {
            return saveName;
        }
        return getDefaultFileName(url, headers);
    }

    private int getDownloadType(Response<ResponseBody> response, String filePath) throws IOException {
        //响应头不包括"Content-Range"即不支持断点续传
        //content-length==-1 表示 chuncked 下载,也不能断点续传
        if (TextUtils.isEmpty(response.headers().get("Content-Range")) || response.body().contentLength() == -1) {
            return NORMAL_DOWNLOAD;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return RE_DOWNLOAD;
        }
        if (file.length() != response.body().contentLength()) {
            return RE_DOWNLOAD;
        }
        String tempFilePath = filePath + SUFFIX;
        File tempFile = new File(tempFilePath);
        if (!tempFile.exists()) {
            return RE_DOWNLOAD;
        }
        if (downloadNotComplete(tempFilePath)) {
            return CONTINUE_DOWNLOAD;
        }
        return FILE_ALREADY_DOWNLOADED;
    }

    private boolean downloadNotComplete(String recordFilePath) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(recordFilePath, "rwd");
            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
            long startByte;
            long endByte;
            for (int i = 0; i < MAX_THREADS; i++) {
                startByte = buffer.getLong();
                endByte = buffer.getLong();
                Log.d(TAG, "Record: " + startByte + " - " + endByte);
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

    /**
     * 如果content-disposition指定了文件名,则使用它
     * 若没有则截取URL最后一段为文件名
     *
     * @param url     URL
     * @param headers Response header
     * @return fileName
     */
    private String getDefaultFileName(String url, Headers headers) {
        String fileName = null;
        String temp = headers.get("content-disposition");
        if (!TextUtils.isEmpty(temp)) {
            Matcher m = Pattern.compile(".*filename=(.*)").matcher(temp);
            if (m.find()) {
                fileName = m.group(1);
            }
        }

        if (TextUtils.isEmpty(fileName)) {
            fileName = url.substring(url.lastIndexOf('/') + 1);
        }
        return fileName;
    }

    private void closeUtils(Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }
}
