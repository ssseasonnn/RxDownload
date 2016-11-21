package zlc.season.rxdownload.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import zlc.season.rxdownload.entity.DownloadMission;
import zlc.season.rxdownload.entity.DownloadRecord;
import zlc.season.rxdownload.entity.DownloadStatus;

import static zlc.season.rxdownload.db.Db.RecordTable.COLUMN_ID;
import static zlc.season.rxdownload.db.Db.RecordTable.TABLE_NAME;
import static zlc.season.rxdownload.db.Db.RecordTable.insert;
import static zlc.season.rxdownload.db.Db.RecordTable.update;

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

    public boolean recordNotExists(String url) {
        Cursor cursor = null;
        try {
            cursor = getWritableDatabase().rawQuery("select " + COLUMN_ID + " from " + TABLE_NAME +
                    " where url=?", new String[]{url});
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

    public void closeDataBase() {
        synchronized (databaseLock) {
            readableDatabase = null;
            writableDatabase = null;
            mDbOpenHelper.close();
        }
    }

    public Observable<List<DownloadRecord>> readAllRecords() {
        return Observable.create(new Observable.OnSubscribe<List<DownloadRecord>>() {
            @Override
            public void call(Subscriber<? super List<DownloadRecord>> subscriber) {
                Cursor cursor = null;
                try {
                    cursor = getReadableDatabase().rawQuery("select * from " + TABLE_NAME, new String[]{});
                    List<DownloadRecord> result = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        result.add(Db.RecordTable.read(cursor));
                    }
                    subscriber.onNext(result);
                    subscriber.onCompleted();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<DownloadRecord> readRecord(final String url) {
        return Observable.create(new Observable.OnSubscribe<DownloadRecord>() {
            @Override
            public void call(Subscriber<? super DownloadRecord> subscriber) {
                Cursor cursor = null;
                try {
                    cursor = getReadableDatabase().rawQuery("select * from " + TABLE_NAME +
                            " where " + "url=?", new String[]{url});
                    while (cursor.moveToNext()) {
                        subscriber.onNext(Db.RecordTable.read(cursor));
                    }
                    subscriber.onCompleted();
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