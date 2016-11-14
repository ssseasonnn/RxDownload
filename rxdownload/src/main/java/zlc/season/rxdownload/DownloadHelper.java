package zlc.season.rxdownload;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.HttpException;
import rx.Subscriber;
import rx.exceptions.CompositeException;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static android.text.TextUtils.concat;
import static java.io.File.separator;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:39
 * Download helper 类
 */
class DownloadHelper {
    static final String TAG = "RxDownload";
    static final String TEST_RANGE_SUPPORT = "bytes=0-";

    private static final String TMP_SUFFIX = ".tmp";  //temp file
    private static final String LMF_SUFFIX = ".lmf";  //last modify file
    private static final String CACHE = ".cache";    //cache directory

    private static final int EACH_RECORD_SIZE = 16; //long + long = 8 + 8
    private int RECORD_FILE_TOTAL_SIZE;
    //|*********************|
    //|*****Record  File****|
    //|*********************|
    //|  0L      |     7L   | 0
    //|  8L      |     15L  | 1
    //|  16L     |     31L  | 2
    //|  ...     |     ...  | MAX_THREADS-1
    //|*********************|
    private int MAX_THREADS = 3;

    private int MAX_RETRY_COUNT = 3;

    private String mDefaultSavePath;
    private String mDefaultCachePath;

    private DownloadApi mDownloadApi;

    //Record : { "url" : new String[] { "file path" , "temp file path" , "last modify file path" }}
    private Map<String, String[]> mDownloadRecord;

    DownloadHelper() {
        mDownloadRecord = new HashMap<>();
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * MAX_THREADS;
        mDownloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
        setDefaultSavePath(getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath());
    }

    void setRetrofit(Retrofit retrofit) {
        mDownloadApi = retrofit.create(DownloadApi.class);
    }

    void setDefaultSavePath(String defaultSavePath) {
        mDefaultSavePath = defaultSavePath;
        mDefaultCachePath = concat(defaultSavePath, separator, CACHE).toString();
        File file = new File(mDefaultSavePath);
        file.mkdir();
        File cache = new File(mDefaultCachePath);
        cache.mkdir();
    }

    int getMaxThreads() {
        return MAX_THREADS;
    }

    void setMaxThreads(int MAX_THREADS) {
        this.MAX_THREADS = MAX_THREADS;
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * MAX_THREADS;
    }

    void setMaxRetryCount(int MAX_RETRY_COUNT) {
        this.MAX_RETRY_COUNT = MAX_RETRY_COUNT;
    }

    DownloadApi getDownloadApi() {
        return mDownloadApi;
    }

    void addDownloadRecord(String url, String saveName, String savePath) {
        String cachePath;

        String filePath;
        String tempPath;
        String lmfPath;

        if (!TextUtils.isEmpty(savePath)) {
            cachePath = concat(savePath, separator, CACHE).toString();
            File file = new File(savePath);
            File cache = new File(cachePath);
            file.mkdir();
            cache.mkdir();

            filePath = concat(savePath, separator, saveName).toString();
            tempPath = concat(cachePath, separator, saveName, TMP_SUFFIX).toString();
            lmfPath = concat(cachePath, separator, saveName, LMF_SUFFIX).toString();
        } else {
            filePath = concat(mDefaultSavePath, separator, saveName).toString();
            tempPath = concat(mDefaultCachePath, separator, saveName, TMP_SUFFIX).toString();
            lmfPath = concat(mDefaultCachePath, separator, saveName, LMF_SUFFIX).toString();
        }

        mDownloadRecord.put(url, new String[]{filePath, tempPath, lmfPath});
    }

    void deleteDownloadRecord(String url) {
        mDownloadRecord.remove(url);
    }

    String getLastModify(String url) throws IOException {
        RandomAccessFile record = null;
        try {
            record = new RandomAccessFile(getLastModifyFileBy(url), "rws");
            record.seek(0);
            return Utils.longToGMT(record.readLong());
        } finally {
            Utils.close(record);
        }
    }

    void prepareNormalDownload(String url, long fileLength, String lastModify) throws IOException, ParseException {
        writeLastModify(url, lastModify);
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(getFileBy(url), "rws");
            file.setLength(fileLength);//设置下载文件的长度
        } finally {
            Utils.close(file);
        }
    }

    void saveNormalFile(Subscriber<? super DownloadStatus> sub, String url, Response<ResponseBody> resp) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            try {
                int readLen;
                int downloadSize = 0;
                byte[] buffer = new byte[8192];

                DownloadStatus status = new DownloadStatus();
                inputStream = resp.body().byteStream();
                outputStream = new FileOutputStream(getFileBy(url));

                long contentLength = resp.body().contentLength();
                boolean isChunked = !TextUtils.isEmpty(Utils.transferEncoding(resp));
                if (isChunked || contentLength == -1) {
                    status.isChunked = true;
                }
                status.setTotalSize(contentLength);

                while ((readLen = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, readLen);
                    downloadSize += readLen;
                    status.setDownloadSize(downloadSize);
                    sub.onNext(status);
                }
                outputStream.flush(); // This is important!!!
                sub.onCompleted();
                Log.i(TAG, "Normal download completed!");
            } finally {
                Utils.close(inputStream);
                Utils.close(outputStream);
                Utils.close(resp.body());
            }
        } catch (IOException e) {
            Log.i(TAG, "Normal download failed or cancel!");
            sub.onError(new Throwable(e));
        }
    }

    DownloadRange getDownloadRange(String url) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(getTempFileBy(url), "rws");
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
            Utils.close(channel);
            Utils.close(record);
        }
    }

    void prepareMultiThreadDownload(String url, long fileLength, String lastModify) throws IOException, ParseException {
        writeLastModify(url, lastModify);
        RandomAccessFile rFile = null;
        RandomAccessFile rRecord = null;
        FileChannel channel = null;
        try {
            rFile = new RandomAccessFile(getFileBy(url), "rws");
            rFile.setLength(fileLength);//设置下载文件的长度

            rRecord = new RandomAccessFile(getTempFileBy(url), "rws");
            rRecord.setLength(RECORD_FILE_TOTAL_SIZE); //设置指针记录文件的大小

            channel = rRecord.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);

            long start;
            long end;
            int eachSize = (int) (fileLength / MAX_THREADS);
            for (int i = 0; i < MAX_THREADS; i++) {
                if (i == MAX_THREADS - 1) {
                    start = i * eachSize;
                    end = fileLength - 1;
                } else {
                    start = i * eachSize;
                    end = (i + 1) * eachSize - 1;
                }
                buffer.putLong(start);
                buffer.putLong(end);
            }
        } finally {
            Utils.close(channel);
            Utils.close(rRecord);
            Utils.close(rFile);
        }
    }

    void saveRangeFile(Subscriber<? super DownloadStatus> subscriber, int i, long start, long end,
                       String url, ResponseBody response) {
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

                record = new RandomAccessFile(getTempFileBy(url), "rws");
                recordChannel = record.getChannel();
                MappedByteBuffer recordBuffer = recordChannel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
                long totalSize = recordBuffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
                status.setTotalSize(totalSize);

                save = new RandomAccessFile(getFileBy(url), "rws");
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
                Utils.close(record);
                Utils.close(recordChannel);
                Utils.close(save);
                Utils.close(saveChannel);
                Utils.close(inStream);
                Utils.close(response);
            }
        } catch (IOException e) {
            Log.i(TAG, Thread.currentThread().getName() + " download failed or cancel!");
            subscriber.onError(new Throwable(e));
        }
    }

    boolean downloadNotComplete(String url) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(getTempFileBy(url), "rws");
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
            Utils.close(channel);
            Utils.close(record);
        }
    }

    boolean downloadNotComplete(String url, long contentLength) {
        return getFileBy(url).length() != contentLength;
    }

    boolean needReDownload(String url, long contentLength) throws IOException {
        return tempFileNotExists(url) || tempFileDamaged(url, contentLength);
    }

    boolean downloadFileExists(String url) {
        return getFileBy(url).exists();
    }

    Boolean retry(Integer integer, Throwable throwable) {
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
        } else if (throwable instanceof ConnectException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                Log.w(TAG, concat(Thread.currentThread().getName(), " ", throwable.getMessage(),
                        ". retry to connect ", String.valueOf(integer), " times").toString());
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

    private File getFileBy(String url) {
        return new File(mDownloadRecord.get(url)[0]);
    }

    private boolean tempFileDamaged(String url, long fileLength) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(getTempFileBy(url), "rws");
            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
            long recordTotalSize = buffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
            return recordTotalSize != fileLength;
        } finally {
            Utils.close(channel);
            Utils.close(record);
        }
    }

    private boolean tempFileNotExists(String url) {
        return !getTempFileBy(url).exists();
    }

    private File getTempFileBy(String url) {
        return new File(mDownloadRecord.get(url)[1]);
    }

    private File getLastModifyFileBy(String url) {
        return new File(mDownloadRecord.get(url)[2]);
    }

    private void writeLastModify(String url, String lastModify) throws IOException, ParseException {
        RandomAccessFile record = null;
        try {
            record = new RandomAccessFile(getLastModifyFileBy(url), "rws");
            record.setLength(8);
            record.seek(0);
            record.writeLong(Utils.GMTToLong(lastModify));
        } finally {
            Utils.close(record);
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
}
