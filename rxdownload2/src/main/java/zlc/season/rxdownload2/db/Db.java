package zlc.season.rxdownload2.db;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.Date;

import zlc.season.rxdownload2.entity.DownloadBean;
import zlc.season.rxdownload2.entity.DownloadRecord;
import zlc.season.rxdownload2.entity.DownloadStatus;

import static zlc.season.rxdownload2.function.Utils.empty;


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
        static final String COLUMN_EXTRA1 = "extra1";
        static final String COLUMN_EXTRA2 = "extra2";
        static final String COLUMN_EXTRA3 = "extra3";
        static final String COLUMN_EXTRA4 = "extra4";
        static final String COLUMN_EXTRA5 = "extra5";
        static final String COLUMN_DATE = "date";
        static final String COLUMN_MISSION_ID = "mission_id";

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
                        COLUMN_EXTRA1 + " TEXT," +
                        COLUMN_EXTRA2 + " TEXT," +
                        COLUMN_EXTRA3 + " TEXT," +
                        COLUMN_EXTRA4 + " TEXT," +
                        COLUMN_EXTRA5 + " TEXT," +
                        COLUMN_DATE + " INTEGER NOT NULL, " +
                        COLUMN_MISSION_ID + " TEXT " +
                        " )";

        static final String ALTER_TABLE_ADD_EXTRA1 = "ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_EXTRA1 + " TEXT";
        static final String ALTER_TABLE_ADD_EXTRA2 = "ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_EXTRA2 + " TEXT";
        static final String ALTER_TABLE_ADD_EXTRA3 = "ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_EXTRA3 + " TEXT";
        static final String ALTER_TABLE_ADD_EXTRA4 = "ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_EXTRA4 + " TEXT";
        static final String ALTER_TABLE_ADD_EXTRA5 = "ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_EXTRA5 + " TEXT";
        static final String ALTER_TABLE_ADD_MISSION_ID = "ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_MISSION_ID + " TEXT";

        static ContentValues insert(DownloadBean bean, int flag, String missionId) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_URL, bean.getUrl());
            values.put(COLUMN_SAVE_NAME, bean.getSaveName());
            values.put(COLUMN_SAVE_PATH, bean.getSavePath());
            values.put(COLUMN_DOWNLOAD_FLAG, flag);
            values.put(COLUMN_EXTRA1, bean.getExtra1());
            values.put(COLUMN_EXTRA2, bean.getExtra2());
            values.put(COLUMN_EXTRA3, bean.getExtra3());
            values.put(COLUMN_EXTRA4, bean.getExtra4());
            values.put(COLUMN_EXTRA5, bean.getExtra5());
            values.put(COLUMN_DATE, new Date().getTime());
            if (empty(missionId)) {
                values.put(COLUMN_MISSION_ID, missionId);
            }
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

        static ContentValues update(int flag, String missionId) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_DOWNLOAD_FLAG, flag);
            if (empty(missionId)) {
                values.put(COLUMN_MISSION_ID, missionId);
            }
            return values;
        }

        static ContentValues update(String saveName, String savePath, int flag) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SAVE_NAME, saveName);
            values.put(COLUMN_SAVE_PATH, savePath);
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
            record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            record.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)));
            record.setSaveName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_NAME)));
            record.setSavePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_PATH)));

            boolean isChunked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CHUNKED)) > 0;
            long downloadSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_SIZE));
            long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE));
            record.setStatus(new DownloadStatus(isChunked, downloadSize, totalSize));

            record.setExtra1(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXTRA1)));
            record.setExtra2(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXTRA2)));
            record.setExtra3(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXTRA3)));
            record.setExtra4(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXTRA4)));
            record.setExtra5(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXTRA5)));

            record.setFlag(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_FLAG)));
            record.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
            record.setMissionId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MISSION_ID)));
            return record;
        }
    }
}
