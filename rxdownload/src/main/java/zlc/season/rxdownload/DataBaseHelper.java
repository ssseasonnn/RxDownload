package zlc.season.rxdownload;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static zlc.season.rxdownload.Db.RecordTable.TABLE_NAME;
import static zlc.season.rxdownload.Db.RecordTable.insert;
import static zlc.season.rxdownload.Db.RecordTable.update;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 10:02
 * FIXME
 */
class DataBaseHelper {
    private volatile SQLiteDatabase mWritableDatabase;
    private DbOpenHelper mDbOpenHelper;

    DataBaseHelper(DbOpenHelper dbOpenHelper) {
        this.mDbOpenHelper = dbOpenHelper;
    }

    Observable<List<DownloadRecord>> readAllRecords() {
        return Observable.create(new Observable.OnSubscribe<List<DownloadRecord>>() {
            @Override
            public void call(Subscriber<? super List<DownloadRecord>> subscriber) {
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    db = mDbOpenHelper.getReadableDatabase();
                    cursor = db.rawQuery("select * from " + TABLE_NAME, new String[]{});
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
                    if (db != null) {
                        db.close();
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    Observable<DownloadRecord> readRecord(final String url) {
        return Observable.create(new Observable.OnSubscribe<DownloadRecord>() {
            @Override
            public void call(Subscriber<? super DownloadRecord> subscriber) {
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    db = mDbOpenHelper.getReadableDatabase();
                    cursor = db.rawQuery("select * from " + TABLE_NAME + " where url=?", new String[]{url});
                    while (cursor.moveToNext()) {
                        subscriber.onNext(Db.RecordTable.read(cursor));
                    }
                    subscriber.onCompleted();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (db != null) {
                        db.close();
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }


    boolean recordNotExists(String url) {
        Cursor cursor = null;
        try {
            cursor = getWritableDatabase().rawQuery("select " + Db.RecordTable.COLUMN_ID + " from "
                    + TABLE_NAME + " where url=?", new String[]{url});
            return cursor.getCount() == 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    long insertRecord(String url, String saveName, String savePath, String name, String image) {
        return getWritableDatabase().insert(TABLE_NAME, null, insert(url, saveName, savePath, name, image));
    }

    long updateRecord(String url, DownloadStatus status) {
        return getWritableDatabase().update(TABLE_NAME, update(status), "url=?", new String[]{url});
    }

    long updateRecord(String url, int flag) {
        return getWritableDatabase().update(TABLE_NAME, update(flag), "url=?", new String[]{url});
    }

    int deleteRecord(String url) {
        return getWritableDatabase().delete(TABLE_NAME, "url=?", new String[]{url});
    }

    void closeDataBase() {
        synchronized (this) {
            mWritableDatabase = null;
            mDbOpenHelper.close();
        }
    }

    private SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db = mWritableDatabase;
        if (db == null) {
            synchronized (this) {
                db = mWritableDatabase;
                if (db == null) {
                    db = mWritableDatabase = mDbOpenHelper.getWritableDatabase();
                }
            }
        }
        return db;
    }
}