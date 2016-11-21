package zlc.season.rxdownload.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import zlc.season.rxdownload.entity.DownloadRecord;
import zlc.season.rxdownload.entity.DownloadStatus;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 10:02
 * FIXME
 */
public class DataBaseHelper {
    private volatile SQLiteDatabase mWritableDatabase;
    private DbOpenHelper mDbOpenHelper;

    public DataBaseHelper(DbOpenHelper dbOpenHelper) {
        this.mDbOpenHelper = dbOpenHelper;
    }

    public boolean recordNotExists(String url) {
        Cursor cursor = null;
        try {
            cursor = getWritableDatabase().rawQuery("select " + Db.RecordTable.COLUMN_ID + " from "
                    + Db.RecordTable.TABLE_NAME + " where url=?", new String[]{url});
            return cursor.getCount() == 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long insertRecord(String url, String saveName, String savePath, String name, String image) {
        return getWritableDatabase().insert(Db.RecordTable.TABLE_NAME, null, Db.RecordTable.insert(url, saveName,
                savePath, name, image));
    }

    public long updateRecord(String url, DownloadStatus status) {
        return getWritableDatabase().update(Db.RecordTable.TABLE_NAME, Db.RecordTable.update(status), "url=?", new
                String[]{url});
    }

    public long updateRecord(String url, int flag) {
        return getWritableDatabase().update(Db.RecordTable.TABLE_NAME, Db.RecordTable.update(flag), "url=?", new
                String[]{url});
    }

    public int deleteRecord(String url) {
        return getWritableDatabase().delete(Db.RecordTable.TABLE_NAME, "url=?", new String[]{url});
    }

    public void closeDataBase() {
        synchronized (this) {
            mWritableDatabase = null;
            mDbOpenHelper.close();
        }
    }

    public Observable<List<DownloadRecord>> readAllRecords() {
        return Observable.create(new Observable.OnSubscribe<List<DownloadRecord>>() {
            @Override
            public void call(Subscriber<? super List<DownloadRecord>> subscriber) {
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    db = mDbOpenHelper.getReadableDatabase();
                    cursor = db.rawQuery("select * from " + Db.RecordTable.TABLE_NAME, new String[]{});
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

    public Observable<DownloadRecord> readRecord(final String url) {
        return Observable.create(new Observable.OnSubscribe<DownloadRecord>() {
            @Override
            public void call(Subscriber<? super DownloadRecord> subscriber) {
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    db = mDbOpenHelper.getReadableDatabase();
                    cursor = db.rawQuery("select * from " + Db.RecordTable.TABLE_NAME + " where url=?", new
                            String[]{url});
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