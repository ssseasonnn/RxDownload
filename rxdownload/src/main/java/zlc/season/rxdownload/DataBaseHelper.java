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

    Observable<List<DownloadRecord>> queryDownloadRecords() {
        return Observable.create(new Observable.OnSubscribe<List<DownloadRecord>>() {
            @Override
            public void call(Subscriber<? super List<DownloadRecord>> subscriber) {
                Cursor cursor = db.query("select * from " + Db.DownloadRecordTable.TABLE_NAME);
                List<DownloadRecord> result = new ArrayList<>();
                while (cursor.moveToNext()) {
                    result.add(Db.DownloadRecordTable.getDownloadRecord(cursor));
                }
                subscriber.onNext(result);
                subscriber.onCompleted();
                cursor.close();
            }
        });
    }

    Observable<DownloadStatus> queryDownloadStatus(final String url) {
        return Observable.create(new Observable.OnSubscribe<DownloadStatus>() {
            @Override
            public void call(Subscriber<? super DownloadStatus> subscriber) {
                Cursor cursor = db.query("select * from " + Db.DownloadRecordTable.TABLE_NAME + " where url=?", url);
                while (cursor.moveToNext()) {
                    subscriber.onNext(Db.DownloadRecordTable.getDownloadStatus(cursor));
                }
                subscriber.onCompleted();
                cursor.close();
            }
        });
    }

    boolean checkDownloadRecordNotExists(String url) {
        Cursor cursor = db.query("select " + Db.DownloadRecordTable.COLUMN_ID + " from "
                + Db.DownloadRecordTable.TABLE_NAME + " where url=?", url);
        return cursor.getCount() == 0;
    }

    long insertDownloadRecord(String url) {
        return db.insert(Db.DownloadRecordTable.TABLE_NAME, Db.DownloadRecordTable.createRecordThroughUrl(url));
    }

    long updateDownloadRecord(String url, DownloadStatus status) {
        return db.update(Db.DownloadRecordTable.TABLE_NAME, Db.DownloadRecordTable.createRecord(status), "url=?", url);
    }

    int deleteDownloadRecord(String url) {
        return db.delete(Db.DownloadRecordTable.TABLE_NAME, "url=?", url);
    }
}