package zlc.season.rxdownload;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.Date;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 10:02
 * FIXME
 */
class Db {
    private Db() {
    }

    static final class DownloadRecordTable {
        static final String TABLE_NAME = "download_record";

        static final String COLUMN_ID = "id";
        static final String COLUMN_URL = "url";
        static final String COLUMN_TOTAL_SIZE = "total_size";
        static final String COLUMN_DOWNLOAD_SIZE = "download_size";
        static final String COLUMN_IS_CHUNKED = "is_chunked";
        static final String COLUMN_DATE = "date";


        static final String CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_URL + " TEXT　NOT NULL," +
                        COLUMN_TOTAL_SIZE + " INTEGER," +
                        COLUMN_DOWNLOAD_SIZE + " INTEGER," +
                        COLUMN_IS_CHUNKED + " INTEGER," +
                        COLUMN_DATE + " INTEGER NOT NULL " +
                        " )";

        static ContentValues createRecordThroughUrl(String url) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_URL, url);
            values.put(COLUMN_DATE, new Date().getTime());
            return values;
        }

        static ContentValues createRecord(DownloadStatus status) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_IS_CHUNKED, status.isChunked);
            values.put(COLUMN_DOWNLOAD_SIZE, status.getDownloadSize());
            values.put(COLUMN_TOTAL_SIZE, status.getTotalSize());
            return values;
        }

        static DownloadRecord getDownloadRecord(Cursor cursor) {
            DownloadRecord record = new DownloadRecord();
            record.setDownloadUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)));
            boolean isChunked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CHUNKED)) > 0;
            long downloadSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_SIZE));
            long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE));
            record.setStatus(new DownloadStatus(isChunked, downloadSize, totalSize));
            record.setDate(Utils.longToGMT(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE))));
            return record;
        }

        static DownloadStatus getDownloadStatus(Cursor cursor) {
            DownloadStatus downloadStatus = new DownloadStatus();
            downloadStatus.isChunked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CHUNKED)) > 0;
            downloadStatus.setDownloadSize(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_SIZE)));
            downloadStatus.setTotalSize(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE)));
            return downloadStatus;
        }
    }
}
