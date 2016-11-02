package zlc.season.rxdownload;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;

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

    private final String RECORD_SUFFIX = ".tmp";
    private final String LAST_MODIFY_SUFFIX = ".lms";

    private final int EACH_RECORD_SIZE = 16; //long + long = 8 + 8

    private int MAX_THREADS = 3;
    private int RECORD_FILE_TOTAL_SIZE;

    //|*********************|
    //|*****Record  File****|
    //|*********************|
    //|  0L      |     7L   | 0
    //|  8L      |     15L  | 1
    //|  16L     |     31L  | 2
    //|  ...     |     ...  | MAX_THREADS-1
    //|*********************|
    //|LastModify|    keep  |
    //|*********************|

    FileHelper() {
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * MAX_THREADS;
    }


    String getSuffix() {
        return RECORD_SUFFIX;
    }

    void setMaxThreads(int MAX_THREADS) {
        this.MAX_THREADS = MAX_THREADS;
        RECORD_FILE_TOTAL_SIZE = EACH_RECORD_SIZE * MAX_THREADS;
    }

    void writeLastModify(String filePath, String lastModify) {
        RandomAccessFile file = null;
        try {
            try {
                file = new RandomAccessFile(filePath + LAST_MODIFY_SUFFIX, "rw");
                file.seek(0);
                file.writeLong(Utils.GMTToLong(lastModify));
            } finally {
                Utils.close(file);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Last Modify File is not found", e);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse String last modify to long", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write last modify", e);
        }
    }

    String getLastModify(String filePath) {
        RandomAccessFile file = null;
        try {
            try {
                file = new RandomAccessFile(filePath + LAST_MODIFY_SUFFIX, "r");
                file.seek(0);
                long temp = file.readLong();
                return Utils.longToGMT(temp);
            } finally {
                Utils.close(file);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Last Modify File is not found", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write last modify", e);
        }
    }

    void prepareNormalDownload(String filePath, long fileLength) {
        RandomAccessFile file = null;
        try {
            try {
                file = new RandomAccessFile(filePath, "rw");
                file.setLength(fileLength);//设置下载文件的长度
            } finally {
                Utils.close(file);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Failed to create normal save file", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare normal download", e);
        }
    }

    void prepareMultiThreadDownload(String filePath, long fileLength) {
        RandomAccessFile file = null;
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            try {
                file = new RandomAccessFile(filePath, "rw");
                file.setLength(fileLength);//设置下载文件的长度

                record = new RandomAccessFile(filePath + RECORD_SUFFIX, "rw");
                record.setLength(RECORD_FILE_TOTAL_SIZE); //设置指针记录文件的大小

                channel = record.getChannel();
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
                Utils.close(record);
                Utils.close(file);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Save File  or Record File is not found", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare multi thread download", e);
        }
    }

    DownloadRange getDownloadRange(String filePath) {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            try {
                record = new RandomAccessFile(filePath + RECORD_SUFFIX, "rw");
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
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Record File is not found", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get download range", e);
        }
    }

    void saveRangeFile(Subscriber<? super DownloadStatus> subscriber, int i, long start, long end,
                       String filePath, ResponseBody response) {
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

                record = new RandomAccessFile(filePath + RECORD_SUFFIX, "rw");
                recordChannel = record.getChannel();
                MappedByteBuffer recordBuffer = recordChannel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
                long totalSize = recordBuffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
                status.setTotalSize(totalSize);

                save = new RandomAccessFile(filePath, "rw");
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
        } catch (FileNotFoundException e) {
            subscriber.onError(new Throwable("Save File  or Record File is not found", e));
        } catch (IOException e) {
            subscriber.onError(new Throwable("Failed to save range file", e));
        }
    }

    void saveNormalFile(Subscriber<? super DownloadStatus> subscriber, String filePath,
                        Response<ResponseBody> response) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            try {
                int readLen;
                int downloadSize = 0;
                byte[] buffer = new byte[8192];

                DownloadStatus status = new DownloadStatus();
                inputStream = response.body().byteStream();

                File file = new File(filePath);
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
                Utils.close(inputStream);
                Utils.close(outputStream);
                Utils.close(response.body());
            }
        } catch (FileNotFoundException e) {
            subscriber.onError(new Throwable("Normal File is not found", e));
        } catch (IOException e) {
            subscriber.onError(new Throwable("Failed to save normal file", e));
        }
    }

    boolean recordFileDamaged(String filePath, long fileLength) {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            try {
                record = new RandomAccessFile(filePath + RECORD_SUFFIX, "rw");
                channel = record.getChannel();
                MappedByteBuffer buffer = channel.map(READ_WRITE, 0, RECORD_FILE_TOTAL_SIZE);
                long recordTotalSize = buffer.getLong(RECORD_FILE_TOTAL_SIZE - 8) + 1;
                return recordTotalSize != fileLength;
            } finally {
                Utils.close(channel);
                Utils.close(record);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Record File is not found", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to check whether the record file is damaged", e);
        }
    }

    boolean downloadNotComplete(String filePath) {
        RandomAccessFile record = null;
        FileChannel channel = null;
        try {
            try {
                record = new RandomAccessFile(filePath + RECORD_SUFFIX, "rw");
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
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Record File is not found", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to check whether downloaded", e);
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
