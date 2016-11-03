package zlc.season.rxdownload;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Subscriber;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:39
 * FIXME
 */
class FileHelper {
    private static final String TAG = "RxDownload";

    private static final String RECORD_FILE_SUFFIX = ".tmp";
    private static final String RECORD_FILE_DIRECTORY = ".cache";

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

    private int MAX_RETRY_COUNT = 3;
    private int MAX_THREADS = 3;

    private String mDefaultPath;
    private String mDefaultRecordPath;

    private DownloadApi mDownloadApi;

    //Record : { "url" : new String[] { "file path" , "temp file path" }}
    private Map<String, String[]> mDownloadRecord;


    FileHelper() {
        mDownloadRecord = new HashMap<>();
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * MAX_THREADS;
    }

    int getMaxThreads() {
        return MAX_THREADS;
    }

    void setMaxThreads(int MAX_THREADS) {
        this.MAX_THREADS = MAX_THREADS;
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * MAX_THREADS;
    }

    int getMaxRetryCount() {
        return MAX_RETRY_COUNT;
    }

    void setMaxRetryCount(int MAX_RETRY_COUNT) {
        this.MAX_RETRY_COUNT = MAX_RETRY_COUNT;
    }

    DownloadApi getDownloadApi() {
        return mDownloadApi;
    }

    void setDownloadApi(DownloadApi downloadApi) {
        this.mDownloadApi = downloadApi;
    }

    String getDefaultPath() {
        return mDefaultPath;
    }

    void setDefaultPath(String defaultPath) {
        mDefaultPath = defaultPath;
        mDefaultRecordPath = defaultPath + File.separator + RECORD_FILE_DIRECTORY;
        File file = new File(mDefaultPath);
        file.mkdir();
        File record = new File(mDefaultRecordPath);
        record.mkdir();
    }


    void addDownloadRecord(String url, String saveName, String savePath) {
        String mFilePath;
        String mRecordFilePath;

        if (!TextUtils.isEmpty(savePath)) {
            File file = new File(savePath);
            if (file.exists()) {
                if (file.isDirectory()) {
                    mFilePath = savePath + File.separator + saveName;
                    mRecordFilePath = savePath + File.separator + RECORD_FILE_DIRECTORY + File.separator +
                            saveName + RECORD_FILE_SUFFIX;
                } else {
                    throw new IllegalArgumentException("Please give me a Directory path, not file path!");
                }
            } else {
                boolean flag = file.mkdir();
                if (!flag) {
                    Log.i(TAG, "create file save path success");
                    mFilePath = savePath + File.separator + saveName;
                    mRecordFilePath = savePath + File.separator + RECORD_FILE_DIRECTORY + File.separator +
                            saveName + RECORD_FILE_SUFFIX;
                } else {
                    Log.i(TAG, "create file save path failed , now use default save path");
                    mFilePath = mDefaultPath + File.separator + saveName;
                    mRecordFilePath = mDefaultRecordPath + File.separator + saveName + RECORD_FILE_SUFFIX;
                }
            }
        } else {
            mFilePath = mDefaultPath + File.separator + saveName;
            mRecordFilePath = mDefaultRecordPath + File.separator + saveName + RECORD_FILE_SUFFIX;
        }

        mDownloadRecord.put(url, new String[]{mFilePath, mRecordFilePath});
    }

    String getLastModify(String url) throws IOException {
        RandomAccessFile record = null;
        try {
            record = new RandomAccessFile(getRecordFile(url), "rws");
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
            file = new RandomAccessFile(getFile(url), "rws");
            file.setLength(fileLength);//设置下载文件的长度
        } finally {
            Utils.close(file);
        }
    }

    void prepareMultiThreadDownload(String url, long fileLength, String lastModify) throws IOException, ParseException {
        writeLastModify(url, lastModify);
        RandomAccessFile rFile = null;
        RandomAccessFile rRecord = null;
        FileChannel channel = null;
        try {
            rFile = new RandomAccessFile(getFile(url), "rws");
            rFile.setLength(fileLength);//设置下载文件的长度

            rRecord = new RandomAccessFile(getRecordFile(url), "rws");
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

    DownloadRange getDownloadRange(String url) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(mDownloadRecord.get(url)[1], "rws");
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

                record = new RandomAccessFile(getRecordFile(url), "rws");
                recordChannel = record.getChannel();
                MappedByteBuffer recordBuffer = recordChannel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
                long totalSize = recordBuffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
                status.setTotalSize(totalSize);

                save = new RandomAccessFile(getFile(url), "rws");
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
            subscriber.onError(new Throwable("Range download stopped! Failed to save range file!", e));
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
                outputStream = new FileOutputStream(getFile(url));

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
            sub.onError(new Throwable("Normal download stopped! Failed to save normal file!", e));
        }
    }

    boolean recordFileDamaged(String url, long fileLength) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(getRecordFile(url), "rws");
            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
            long recordTotalSize = buffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
            return recordTotalSize != fileLength;
        } finally {
            Utils.close(channel);
            Utils.close(record);
        }
    }

    boolean downloadNotComplete(String url) throws IOException {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(getRecordFile(url), "rws");
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

    boolean recordFileNotExists(String url) {
        return !getRecordFile(url).exists();
    }

    File getFile(String url) {
        return new File(mDownloadRecord.get(url)[0]);
    }

    void subtractRecord(String url) {
        mDownloadRecord.remove(url);
    }

    private File getRecordFile(String url) {
        return new File(mDownloadRecord.get(url)[1]);
    }

    private void writeLastModify(String url, String lastModify) throws IOException, ParseException {
        RandomAccessFile record = null;
        try {
            record = new RandomAccessFile(getRecordFile(url), "rws");
            record.setLength(8);
            record.seek(0);
            record.writeLong(Utils.GMTToLong(lastModify));
        } finally {
            Utils.close(record);
        }
    }

    private File createFileAndSet(String filePath, String lastModify) throws IOException, ParseException {
        File file = new File(filePath);
        if (file.exists()) {
            boolean delete = file.delete();
            if (!delete) {
                throw new IOException("delete download file failed");
            }
        }
        boolean create = file.createNewFile();
        if (!create) {
            throw new IOException("create download file failed");
        }
        boolean read = file.setReadable(true);
        if (!read) {
            throw new IOException("set download file read permission failed");
        }
        boolean write = file.setWritable(true);
        if (!write) {
            throw new IOException("set download file write permission failed");
        }
        boolean writeLM = file.setLastModified(Utils.GMTToLong(lastModify));
        if (!writeLM) {
            throw new IOException("set download file last modify failed");
        }
        return file;
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
