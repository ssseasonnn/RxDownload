package zlc.season.rxdownload;

import android.database.Cursor;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 10:02
 * FIXME
 */
class DataBaseHelper {
    private final BriteDatabase db;

    DataBaseHelper(DbOpenHelper dbOpenHelper) {
        this.db = new SqlBrite.Builder().build().wrapDatabaseHelper(dbOpenHelper, Schedulers.io());
    }

    Observable<List<DownloadRecord>> readAllRecords() {
        return Observable.create(new Observable.OnSubscribe<List<DownloadRecord>>() {
            @Override
            public void call(Subscriber<? super List<DownloadRecord>> subscriber) {
                Cursor cursor = db.query("select * from " + Db.RecordTable.TABLE_NAME);
                List<DownloadRecord> result = new ArrayList<>();
                while (cursor.moveToNext()) {
                    result.add(Db.RecordTable.read(cursor));
                }
                subscriber.onNext(result);
                subscriber.onCompleted();
                cursor.close();
            }
        });
    }

    Observable<DownloadRecord> readRecord(final String url) {
        return Observable.create(new Observable.OnSubscribe<DownloadRecord>() {
            @Override
            public void call(Subscriber<? super DownloadRecord> subscriber) {
                Cursor cursor = db.query("select * from " + Db.RecordTable.TABLE_NAME + " where url=?", url);
                while (cursor.moveToNext()) {
                    subscriber.onNext(Db.RecordTable.read(cursor));
                }
                subscriber.onCompleted();
                cursor.close();
            }
        });
    }

    boolean recordNotExists(String url) {
        Cursor cursor = db.query("select " + Db.RecordTable.COLUMN_ID + " from "
                + Db.RecordTable.TABLE_NAME + " where url=?", url);
        return cursor.getCount() == 0;
    }

    long insertRecord(String url, String saveName, String savePath, String name, String image) {
        return db.insert(Db.RecordTable.TABLE_NAME, Db.RecordTable.insert(url, saveName, savePath, name, image));
    }

    long updateRecord(String url, DownloadStatus status) {
        return db.update(Db.RecordTable.TABLE_NAME, Db.RecordTable.update(status), "url=?", url);
    }

    long updateRecord(String url, int flag) {
        return db.update(Db.RecordTable.TABLE_NAME, Db.RecordTable.update(flag), "url=?", url);
    }

    int deleteRecord(String url) {
        return db.delete(Db.RecordTable.TABLE_NAME, "url=?", url);
    }
}