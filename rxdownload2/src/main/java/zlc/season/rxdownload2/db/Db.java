package zlc.season.rxdownload2.db;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.Date;

import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadMission;
import zlc.season.rxdownload2.entity.DownloadRecord;
import zlc.season.rxdownload2.entity.DownloadStatus;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 10:02
 * FIXME
 */
class Db {
    private Db() {
    }

    static final class RecordTable {
        static final String TABLE_NAME = "download_record";

        static final String COLUMN_ID = "id";
        static final String COLUMN_URL = "url";
        static final String COLUMN_SAVE_NAME = "save_name";
        static final String COLUMN_SAVE_PATH = "save_path";
        static final String COLUMN_DOWNLOAD_SIZE = "download_size";
        static final String COLUMN_TOTAL_SIZE = "total_size";
        static final String COLUMN_IS_CHUNKED = "is_chunked";
        static final String COLUMN_DOWNLOAD_FLAG = "download_flag";
        static final String COLUMN_DATE = "date";

        //编译器会自动优化为StringBuild方式,不用担心效率问题
        static final String CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_URL + " TEXT NOT NULL," +
                        COLUMN_SAVE_NAME + " TEXT," +
                        COLUMN_SAVE_PATH + " TEXT," +
                        COLUMN_TOTAL_SIZE + " INTEGER," +
                        COLUMN_DOWNLOAD_SIZE + " INTEGER," +
                        COLUMN_IS_CHUNKED + " INTEGER," +
                        COLUMN_DOWNLOAD_FLAG + " INTEGER," +
                        COLUMN_DATE + " INTEGER NOT NULL " +
                        " )";

        static ContentValues insert(DownloadMission mission) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_URL, mission.getUrl());
            values.put(COLUMN_SAVE_NAME, mission.getSaveName());
            values.put(COLUMN_SAVE_PATH, mission.getSavePath());
            values.put(COLUMN_DOWNLOAD_FLAG, DownloadFlag.WAITING);
            values.put(COLUMN_DATE, new Date().getTime());
            return values;
        }

        static ContentValues update(DownloadStatus status) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_IS_CHUNKED, status.isChunked);
            values.put(COLUMN_DOWNLOAD_SIZE, status.getDownloadSize());
            values.put(COLUMN_TOTAL_SIZE, status.getTotalSize());
            return values;
        }

        static ContentValues update(int flag) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_DOWNLOAD_FLAG, flag);
            return values;
        }

        static DownloadStatus readStatus(Cursor cursor) {
            boolean isChunked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CHUNKED)) > 0;
            long downloadSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_SIZE));
            long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE));
            return new DownloadStatus(isChunked, downloadSize, totalSize);
        }

        static DownloadRecord read(Cursor cursor) {
            DownloadRecord record = new DownloadRecord();
            record.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)));
            record.setSaveName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_NAME)));
            record.setSavePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_PATH)));

            boolean isChunked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CHUNKED)) > 0;
            long downloadSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_SIZE));
            long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE));
            record.setStatus(new DownloadStatus(isChunked, downloadSize, totalSize));

            record.setFlag(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_FLAG)));
            record.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
            return record;
        }
    }
}
