package zlc.season.rxdownload;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

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
    private static final int MULTI_THREAD_DOWNLOAD = 1;
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
        Log.d("Main", Thread.currentThread().getName());
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

    /**
     * 开始下载
     *
     * @param url             下载文件的Url
     * @param saveName        下载文件的保存名称, null使用默认的名称(从url或响应头中读取文件名)
     * @param savePath        下载文件的保存路径, null使用默认的路径,默认保存在/sdcard/Download/目录下
     * @param forceReDownload 强制重新下载
     * @return Observable
     */
    public Observable<DownloadStatus> download(final String url, final String saveName, final String savePath,
                                               final boolean forceReDownload) {
        Log.d("enter download", Thread.currentThread().getName());
        beforeDownload();
        return mDownloadApi.download(RANGE_TEST, url)
                //                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(final Response<ResponseBody> response) {
                        try {
                            Log.d("enter flat map", Thread.currentThread().getName());
                            return createDownloadObservable(response, saveName, savePath, url, forceReDownload);
                        } catch (IOException e) {
                            Log.w(TAG, e);
                            return Observable.error(new Throwable(e));
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

        RECORD_FILE_TOTAL_SIZE = MAX_THREADS * EACH_RECORD_SIZE;

        mDownloadApi = mRetrofit.create(DownloadApi.class);
    }

    private Observable<DownloadStatus> createDownloadObservable(Response<ResponseBody> response, String saveName,
                                                                String savePath, String url,
                                                                boolean forceReDownload) throws IOException {
        Log.d("enter create download", Thread.currentThread().getName());
        String fileName = getFileSaveName(saveName, url, response.headers());
        String filePath = getFileSavePath(savePath) + File.separator + fileName;

        final long contentLength = response.body().contentLength();
        Log.i(TAG, "Download file size is: " + contentLength + "!");

        int type = getDownloadType(response, filePath, forceReDownload);
        switch (type) {
            case NORMAL_DOWNLOAD:
                Log.i(TAG, "Normal download start!");
                return startNormalDownload(filePath, response);
            case MULTI_THREAD_DOWNLOAD:
                /**
                 * prepare download
                 */
                prepareDownload(filePath, contentLength);
                Log.i(TAG, "Multi thread download start!");
                closeUtils(response.body());
                return startMultiThreadDownload(filePath, url);
            case CONTINUE_DOWNLOAD:
                Log.i(TAG, "Continue download start!");
                closeUtils(response.body());
                return startMultiThreadDownload(filePath, url);
            case FILE_ALREADY_DOWNLOADED:
                closeUtils(response.body());
                Log.i(TAG, "Already downloaded!");
                return Observable.just(new DownloadStatus(contentLength, contentLength));
            default:
                Log.i(TAG, "unknown error!");
                closeUtils(response.body());
                return Observable.error(new Throwable("unknown error!"));
        }
    }

    /**
     * 常规下载, 不采用多线程和断点续传
     *
     * @param savePath 下载文件保存路径
     * @param response Response
     * @return Observable
     */
    private Observable<DownloadStatus> startNormalDownload(final String savePath, final Response<ResponseBody>
            response) {
        Log.d("enter start normal", Thread.currentThread().getName());
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                try {
                    Log.d("enter create", Thread.currentThread().getName());
                    specificSaveNormalFile(subscriber, savePath, response);
                } catch (IOException e) {
                    Log.w(TAG, e);
                    subscriber.onError(e);
                }
            }
        }).sample(500, TimeUnit.MILLISECONDS).doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.w(TAG, throwable);
            }
        });
    }

    private void specificSaveNormalFile(Subscriber<? super DownloadStatus> subscriber, String savePath,
                                        Response<ResponseBody> response) throws IOException {
        Log.d("enter save normal", Thread.currentThread().getName());
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            int readLen;
            byte[] buffer = new byte[8192];
            File file = new File(savePath);
            DownloadStatus status = new DownloadStatus();

            inputStream = response.body().byteStream();
            outputStream = new FileOutputStream(file);

            long contentLength = response.body().contentLength();
            boolean isChunked = !TextUtils.isEmpty(response.headers().get("Transfer-Encoding"));
            if (isChunked || contentLength == -1) {
                status.isChunked = true;
            }
            status.totalSize = contentLength;

            while ((readLen = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readLen);
                status.downloadSize += readLen;
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
    }

    /**
     * 多线程+断点续传
     *
     * @param filePath filePath
     * @param url      url
     * @return Observable
     * @throws IOException
     */
    private Observable<DownloadStatus> startMultiThreadDownload(String filePath,
                                                                String url) throws IOException {
        Log.d("enter start multi", Thread.currentThread().getName());
        DownloadRange range = getDownloadRange(filePath);

        //        if (MAX_THREADS == 1) {
        //            return rangeDownloadTask(range.start[0],range.end[0],0,url,filePath);
        //        }
        //
        List<Observable<DownloadStatus>> tasks = new ArrayList<>();
        for (int i = 0; i < MAX_THREADS; i++) {
            if (range.start[i] <= range.end[i]) {
                tasks.add(rangeDownloadTask(range.start[i], range.end[i], i, url, filePath));
            }
        }

        Log.d("before combine", Thread.currentThread().getName());
        return Observable.combineLatestDelayError(tasks, new FuncN<DownloadStatus>() {
            @Override
            public DownloadStatus call(Object... args) {
                return getDownloadStatus(args);
            }
        })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.w(TAG, throwable);
                    }
                });
    }

    @NonNull
    private DownloadStatus getDownloadStatus(Object[] args) {
        Log.d("enter combine", Thread.currentThread().getName());
        DownloadStatus total = (DownloadStatus) args[0];
        for (int i = 1; i < args.length; i++) {
            DownloadStatus temp = (DownloadStatus) args[i];
            total.downloadSize += temp.downloadSize;
            total.totalSize += temp.totalSize;
        }
        return total;
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
        Log.d("enter create range task", Thread.currentThread().getName());
        String range = "bytes=" + start + "-" + end;
        return mDownloadApi.download(range, url)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Response<ResponseBody>, Observable<DownloadStatus>>() {
                    @Override
                    public Observable<DownloadStatus> call(Response<ResponseBody> response) {
                        Log.d("enter create range flat", Thread.currentThread().getName());
                        return saveRangeFile(start, end, i, filePath, response.body());
                    }
                }).sample(500, TimeUnit.MILLISECONDS);
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
        Log.d("enter save range", Thread.currentThread().getName());
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                try {
                    Log.d("enter create", Thread.currentThread().getName());
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
        Log.d("enter save file", Thread.currentThread().getName());
        RandomAccessFile record = null;
        FileChannel recordChannel = null;

        RandomAccessFile save = null;
        FileChannel saveChannel = null;

        InputStream inStream = null;
        try {
            Log.i(TAG, Thread.currentThread().getName() + " start download from " + start + " to " + end + "!");
            byte[] buffer = new byte[8192];
            int readLen;
            DownloadStatus status = new DownloadStatus();
            status.totalSize = response.contentLength();

            record = new RandomAccessFile(filePath + SUFFIX, "rwd");
            recordChannel = record.getChannel();
            MappedByteBuffer recordBuffer = recordChannel.map(READ_WRITE, i * EACH_RECORD_SIZE, EACH_RECORD_SIZE);

            save = new RandomAccessFile(filePath, "rwd");
            saveChannel = save.getChannel();
            MappedByteBuffer saveBuffer = saveChannel.map(READ_WRITE, start, end - start + 1);

            inStream = response.byteStream();
            while ((readLen = inStream.read(buffer)) != -1) {
                saveBuffer.put(buffer, 0, readLen);
                recordBuffer.putLong(0, recordBuffer.getLong(0) + readLen);
                status.downloadSize += readLen;
                Log.d("while", Thread.currentThread().getName());
                Log.d(TAG, "send data" + readLen);
                subscriber.onNext(status);
            }
            Log.i(TAG, Thread.currentThread().getName() + " complete download! Download size is " +
                    status.downloadSize + " bytes");
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

    /**
     * 判断下载类型
     *
     * @param response        Http Response
     * @param filePath        File Save Path
     * @param forceReDownload 是否强制重新下载,如果为true,不管以前是否下载过该文件,都重新下载
     * @return ##
     * {@link RxDownload#NORMAL_DOWNLOAD}  常规下载,单线程,无断点续传
     * {@link RxDownload#MULTI_THREAD_DOWNLOAD} 多线程下载+断点续传
     * {@link RxDownload#CONTINUE_DOWNLOAD} 断点续传
     * {@link RxDownload#FILE_ALREADY_DOWNLOADED} 文件已经下载成功,无需重新下载
     * @throws IOException
     */
    private int getDownloadType(Response<ResponseBody> response, String filePath, boolean forceReDownload) throws
            IOException {
        //响应头有 "Content-Range: bytes 0-12710467/38131405" 字段, 并且Content-Length 不为 -1, 表示支持断点续传,否则不支持
        //响应头有 "Transfer-Encoding : chunked" 字段,或者Content-Length为-1 , 表示分块传输,不能使用断点续传;
        boolean notSupportRangeDownload = TextUtils.isEmpty(response.headers().get("Content-Range")) ||
                response.body().contentLength() == -1;

        if (notSupportRangeDownload) {
            if (forceReDownload) {
                return NORMAL_DOWNLOAD;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                return NORMAL_DOWNLOAD;
            }
            if (file.length() != response.body().contentLength()) {
                return NORMAL_DOWNLOAD;
            }
            return FILE_ALREADY_DOWNLOADED;
        }


        File file = new File(filePath);
        if (!file.exists()) {
            return MULTI_THREAD_DOWNLOAD;
        }
        if (file.length() != response.body().contentLength()) {
            return MULTI_THREAD_DOWNLOAD;
        }
        String recordPath = filePath + SUFFIX;
        File recordFile = new File(recordPath);
        if (!recordFile.exists()) {
            return MULTI_THREAD_DOWNLOAD;
        }

        if (forceReDownload) {
            boolean delete = recordFile.delete();
            if (delete) {
                Log.d(TAG, "record file is delete success");
            } else {
                Log.d(TAG, "record file is delete failed");
            }
            return MULTI_THREAD_DOWNLOAD;
        }

        if (downloadNotComplete(recordPath)) {
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
