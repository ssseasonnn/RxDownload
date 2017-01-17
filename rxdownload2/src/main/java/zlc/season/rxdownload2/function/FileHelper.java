package zlc.season.rxdownload2.function;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;

import io.reactivex.FlowableEmitter;
import okhttp3.ResponseBody;
import retrofit2.Response;
import zlc.season.rxdownload2.entity.DownloadRange;
import zlc.season.rxdownload2.entity.DownloadStatus;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static android.text.TextUtils.concat;
import static java.io.File.separator;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static zlc.season.rxdownload2.function.Constant.CHUNKED_DOWNLOAD_HINT;
import static zlc.season.rxdownload2.function.Utils.GMTToLong;
import static zlc.season.rxdownload2.function.Utils.closeQuietly;
import static zlc.season.rxdownload2.function.Utils.isChunked;
import static zlc.season.rxdownload2.function.Utils.log;
import static zlc.season.rxdownload2.function.Utils.longToGMT;
import static zlc.season.rxdownload2.function.Utils.mkdirs;
import static zlc.season.rxdownload2.function.Utils.notEmpty;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/17
 * Time: 10:35
 * <p>
 * File Helper
 */
public class FileHelper {

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

    private String mDefaultSavePath;
    private String mDefaultCachePath;

    FileHelper() {
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * MAX_THREADS;
        setDefaultSavePath(getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath());
    }

    void setDefaultSavePath(String defaultSavePath) {
        mDefaultSavePath = defaultSavePath;
        mDefaultCachePath = concat(defaultSavePath, separator, CACHE).toString();
    }

    int getMaxThreads() {
        return MAX_THREADS;
    }

    void setMaxThreads(int MAX_THREADS) {
        this.MAX_THREADS = MAX_THREADS;
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * MAX_THREADS;
    }

    void createDownloadDirs(String savePath)
            throws IOException {
        mkdirs(getRealDirectoryPaths(savePath));
    }

    String[] getRealDirectoryPaths(String savePath) {
        String fileDirectory;
        String cacheDirectory;
        if (notEmpty(savePath)) {
            fileDirectory = savePath;
            cacheDirectory = concat(savePath, separator, CACHE).toString();
        } else {
            fileDirectory = mDefaultSavePath;
            cacheDirectory = mDefaultCachePath;
        }
        return new String[]{fileDirectory, cacheDirectory};
    }

    String[] getRealFilePaths(String saveName, String savePath) {
        String[] directories = getRealDirectoryPaths(savePath);
        String filePath = concat(directories[0], separator, saveName).toString();
        String tempPath = concat(directories[1], separator, saveName, TMP_SUFFIX).toString();
        String lmfPath = concat(directories[1], separator, saveName, LMF_SUFFIX).toString();
        return new String[]{filePath, tempPath, lmfPath};
    }

    void prepareDownload(File lastModifyFile, File saveFile,
            long fileLength, String lastModify)
            throws IOException, ParseException {

        writeLastModify(lastModifyFile, lastModify);
        prepareFile(saveFile, fileLength);
    }

    void saveFile(FlowableEmitter<DownloadStatus> emitter,
            File saveFile, Response<ResponseBody> resp) {

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            try {
                int readLen;
                int downloadSize = 0;
                byte[] buffer = new byte[8192];

                DownloadStatus status = new DownloadStatus();
                inputStream = resp.body().byteStream();
                outputStream = new FileOutputStream(saveFile);

                long contentLength = resp.body().contentLength();

                boolean isChunked = isChunked(resp);
                if (isChunked || contentLength == -1) {
                    status.isChunked = true;
                }

                status.setTotalSize(contentLength);

                while ((readLen = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, readLen);
                    downloadSize += readLen;
                    status.setDownloadSize(downloadSize);
                    emitter.onNext(status);
                }

                outputStream.flush(); // This is important!!!
                emitter.onComplete();
            } finally {
                closeQuietly(inputStream);
                closeQuietly(outputStream);
                closeQuietly(resp.body());
            }
        } catch (IOException e) {
            emitter.onError(e);
        }
    }

    void prepareDownload(File lastModifyFile, File tempFile, File saveFile,
            long fileLength, String lastModify)
            throws IOException, ParseException {

        writeLastModify(lastModifyFile, lastModify);
        prepareFile(tempFile, saveFile, fileLength);
    }

    void saveFile(FlowableEmitter<DownloadStatus> emitter,
            int i, long start, long end,
            File tempFile, File saveFile,
            ResponseBody response) {

        RandomAccessFile record = null;
        FileChannel recordChannel = null;
        RandomAccessFile save = null;
        FileChannel saveChannel = null;
        InputStream inStream = null;
        try {
            try {
                int readLen;
                byte[] buffer = new byte[8192];

                DownloadStatus status = new DownloadStatus();
                record = new RandomAccessFile(tempFile, "rws");
                recordChannel = record.getChannel();
                MappedByteBuffer recordBuffer = recordChannel
                        .map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);

                long totalSize = recordBuffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
                status.setTotalSize(totalSize);

                save = new RandomAccessFile(saveFile, "rws");
                saveChannel = save.getChannel();
                MappedByteBuffer saveBuffer = saveChannel.map(READ_WRITE, start, end - start + 1);
                inStream = response.byteStream();

                while ((readLen = inStream.read(buffer)) != -1) {
                    saveBuffer.put(buffer, 0, readLen);
                    recordBuffer.putLong(i * EACH_RECORD_SIZE,
                            recordBuffer.getLong(i * EACH_RECORD_SIZE) + readLen);
                    status.setDownloadSize(totalSize - getResidue(recordBuffer));
                    emitter.onNext(status);
                }
                emitter.onComplete();
            } finally {
                closeQuietly(record);
                closeQuietly(recordChannel);
                closeQuietly(save);
                closeQuietly(saveChannel);
                closeQuietly(inStream);
                closeQuietly(response);
            }
        } catch (IOException e) {
            emitter.onError(e);
        }
    }

    boolean downloadNotComplete(File tempFile)
            throws IOException {

        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(tempFile, "rws");
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
            closeQuietly(channel);
            closeQuietly(record);
        }
    }

    boolean tempFileDamaged(File tempFile, long fileLength)
            throws IOException {

        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(tempFile, "rws");
            channel = record.getChannel();
            MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
            long recordTotalSize = buffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
            return recordTotalSize != fileLength;
        } finally {
            closeQuietly(channel);
            closeQuietly(record);
        }
    }

    DownloadRange readDownloadRange(File tempFile, int i)
            throws IOException {

        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            record = new RandomAccessFile(tempFile, "rws");
            channel = record.getChannel();
            MappedByteBuffer buffer = channel
                    .map(READ_WRITE, i * EACH_RECORD_SIZE, (i + 1) * EACH_RECORD_SIZE);
            long startByte = buffer.getLong();
            long endByte = buffer.getLong();
            return new DownloadRange(startByte, endByte);
        } finally {
            closeQuietly(channel);
            closeQuietly(record);
        }
    }

    String getLastModify(File file)
            throws IOException {

        RandomAccessFile record = null;
        try {
            record = new RandomAccessFile(file, "rws");
            record.seek(0);
            return longToGMT(record.readLong());
        } finally {
            closeQuietly(record);
        }
    }

    private void prepareFile(File tempFile, File saveFile, long fileLength)
            throws IOException {
        RandomAccessFile rFile = null;
        RandomAccessFile rRecord = null;
        FileChannel channel = null;
        try {
            rFile = new RandomAccessFile(saveFile, "rws");
            rFile.setLength(fileLength);//设置下载文件的长度

            rRecord = new RandomAccessFile(tempFile, "rws");
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
            closeQuietly(channel);
            closeQuietly(rRecord);
            closeQuietly(rFile);
        }
    }

    private void prepareFile(File saveFile, long fileLength)
            throws IOException {

        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(saveFile, "rws");
            if (fileLength != -1) {
                file.setLength(fileLength);//设置下载文件的长度
            } else {
                log(CHUNKED_DOWNLOAD_HINT);
                //Chunked 下载, 无需设置文件大小.
            }
        } finally {
            closeQuietly(file);
        }
    }

    private void writeLastModify(File file, String lastModify)
            throws IOException, ParseException {

        RandomAccessFile record = null;
        try {
            record = new RandomAccessFile(file, "rws");
            record.setLength(8);
            record.seek(0);
            record.writeLong(GMTToLong(lastModify));
        } finally {
            closeQuietly(record);
        }
    }

    /**
     * 还剩多少字节没有下载
     *
     * @param recordBuffer buffer
     *
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
