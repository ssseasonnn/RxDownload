package zlc.season.rxdownload2.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadMission;
import zlc.season.rxdownload2.entity.DownloadRecord;
import zlc.season.rxdownload2.entity.DownloadStatus;

import static zlc.season.rxdownload2.db.Db.RecordTable.COLUMN_DOWNLOAD_FLAG;
import static zlc.season.rxdownload2.db.Db.RecordTable.COLUMN_DOWNLOAD_SIZE;
import static zlc.season.rxdownload2.db.Db.RecordTable.COLUMN_ID;
import static zlc.season.rxdownload2.db.Db.RecordTable.COLUMN_IS_CHUNKED;
import static zlc.season.rxdownload2.db.Db.RecordTable.COLUMN_TOTAL_SIZE;
import static zlc.season.rxdownload2.db.Db.RecordTable.TABLE_NAME;
import static zlc.season.rxdownload2.db.Db.RecordTable.insert;
import static zlc.season.rxdownload2.db.Db.RecordTable.update;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 10:02
 * FIXME
 */
public class DataBaseHelper {
    private volatile static DataBaseHelper singleton;
    private final Object databaseLock = new Object();
    private DbOpenHelper mDbOpenHelper;
    private volatile SQLiteDatabase readableDatabase;
    private volatile SQLiteDatabase writableDatabase;

    private DataBaseHelper(Context context) {
        mDbOpenHelper = new DbOpenHelper(context);
    }

    public static DataBaseHelper getSingleton(Context context) {
        if (singleton == null) {
            synchronized (DataBaseHelper.class) {
                if (singleton == null) {
                    singleton = new DataBaseHelper(context);
                }
            }
        }
        return singleton;
    }

    public boolean recordExists(String url) {
        return !recordNotExists(url);
    }

    public boolean recordNotExists(String url) {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, new String[]{COLUMN_ID}, "url=?",
                    new String[]{url}, null, null, null);
            return cursor.getCount() == 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long insertRecord(DownloadMission mission) {
        return getWritableDatabase().insert(TABLE_NAME, null, insert(mission));
    }

    public long updateRecord(String url, DownloadStatus status) {
        return getWritableDatabase().update(TABLE_NAME, update(status), "url=?", new String[]{url});
    }

    public long updateRecord(String url, int flag) {
        return getWritableDatabase().update(TABLE_NAME, update(flag), "url=?", new String[]{url});
    }

    public int deleteRecord(String url) {
        return getWritableDatabase().delete(TABLE_NAME, "url=?", new String[]{url});
    }

    public DownloadRecord readSingleRecord(String url) {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase()
                    .rawQuery("select * from " + TABLE_NAME + " where url=?", new String[]{url});
            cursor.moveToFirst();
            return Db.RecordTable.read(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public DownloadStatus readStatus(String url) {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(
                    TABLE_NAME,
                    new String[]{COLUMN_DOWNLOAD_SIZE, COLUMN_TOTAL_SIZE, COLUMN_IS_CHUNKED},
                    "url=?", new String[]{url}, null, null, null);
            if (cursor.getCount() == 0) {
                return new DownloadStatus();
            } else {
                cursor.moveToFirst();
                return Db.RecordTable.readStatus(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void closeDataBase() {
        synchronized (databaseLock) {
            readableDatabase = null;
            writableDatabase = null;
            mDbOpenHelper.close();
        }
    }

    public Observable<List<DownloadRecord>> readAllRecords() {
        return Observable.create(new ObservableOnSubscribe<List<DownloadRecord>>() {
            @Override
            public void subscribe(ObservableEmitter<List<DownloadRecord>> emitter)
                    throws Exception {
                Cursor cursor = null;
                try {
                    cursor = getReadableDatabase()
                            .rawQuery("select * from " + TABLE_NAME, new String[]{});
                    List<DownloadRecord> result = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        result.add(Db.RecordTable.read(cursor));
                    }
                    emitter.onNext(result);
                    emitter.onComplete();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public long repairErrorFlag() {
        return getWritableDatabase().update(TABLE_NAME, update(DownloadFlag.PAUSED),
                COLUMN_DOWNLOAD_FLAG + "=? or " + COLUMN_DOWNLOAD_FLAG + "=?",
                new String[]{DownloadFlag.WAITING + "", DownloadFlag.STARTED + ""});
    }

    /**
     * 获得url对应的下载记录
     * <p>
     * ps: 如果数据库中没有记录，则返回一个空的DownloadRecord.
     *
     * @param url url
     *
     * @return record
     */
    public Observable<DownloadRecord> readRecord(final String url) {
        return Observable.create(new ObservableOnSubscribe<DownloadRecord>() {
            @Override
            public void subscribe(ObservableEmitter<DownloadRecord> emitter) throws Exception {
                Cursor cursor = null;
                try {
                    cursor = getReadableDatabase().rawQuery("select * from " + TABLE_NAME +
                            " where " + "url=?", new String[]{url});
                    if (cursor.getCount() == 0) {
                        emitter.onNext(new DownloadRecord());
                    } else {
                        cursor.moveToFirst();
                        emitter.onNext(Db.RecordTable.read(cursor));
                    }
                    emitter.onComplete();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db = writableDatabase;
        if (db == null) {
            synchronized (databaseLock) {
                db = writableDatabase;
                if (db == null) {
                    db = writableDatabase = mDbOpenHelper.getWritableDatabase();
                }
            }
        }
        return db;
    }

    private SQLiteDatabase getReadableDatabase() {
        SQLiteDatabase db = readableDatabase;
        if (db == null) {
            synchronized (databaseLock) {
                db = readableDatabase;
                if (db == null) {
                    db = readableDatabase = mDbOpenHelper.getReadableDatabase();
                }
            }
        }
        return db;
    }
}