package zlc.season.rxdownload;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.Date;

import static zlc.season.rxdownload.DownloadRecord.FLAG_STARTED;


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
        static final String COLUMN_NAME = "name";
        static final String COLUMN_IMAGE = "image";
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
                        COLUMN_NAME + " TEXT," +
                        COLUMN_IMAGE + " TEXT," +
                        COLUMN_SAVE_NAME + " TEXT," +
                        COLUMN_SAVE_PATH + " TEXT," +
                        COLUMN_TOTAL_SIZE + " INTEGER," +
                        COLUMN_DOWNLOAD_SIZE + " INTEGER," +
                        COLUMN_IS_CHUNKED + " INTEGER," +
                        COLUMN_DOWNLOAD_FLAG + " INTEGER," +
                        COLUMN_DATE + " INTEGER NOT NULL " +
                        " )";


        static ContentValues insert(String url, String saveName, String savePath,
                                    String name, String image) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_URL, url);
            values.put(COLUMN_SAVE_NAME, saveName);
            values.put(COLUMN_SAVE_PATH, savePath);
            values.put(COLUMN_NAME, TextUtils.isEmpty(name) ? "" : name);
            values.put(COLUMN_IMAGE, TextUtils.isEmpty(image) ? "" : image);
            values.put(COLUMN_DOWNLOAD_FLAG, FLAG_STARTED);
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

        static ContentValues update(int status) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_DOWNLOAD_FLAG, status);
            return values;
        }

        static DownloadRecord read(Cursor cursor) {
            DownloadRecord record = new DownloadRecord();
            record.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)));
            record.setSaveName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_NAME)));
            record.setSavePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_PATH)));
            record.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            record.setImage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE)));

            boolean isChunked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CHUNKED)) > 0;
            long downloadSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_SIZE));
            long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE));
            record.setStatus(new DownloadStatus(isChunked, downloadSize, totalSize));

            record.setDownloadFlag(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_FLAG)));
            record.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
            return record;
        }
    }
}
