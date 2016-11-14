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
public class DataBaseHelper {
    private final BriteDatabase db;

    public DataBaseHelper(DbOpenHelper dbOpenHelper) {
        this.db = new SqlBrite.Builder().build().wrapDatabaseHelper(dbOpenHelper, Schedulers.io());
    }

    public Observable<List<DownloadRecord>> queryAllDownloadRecord() {
        return Observable.create(new Observable.OnSubscribe<List<DownloadRecord>>() {
            @Override
            public void call(Subscriber<? super List<DownloadRecord>> subscriber) {
                Cursor cursor = db.query("select * from " + Db.DownloadRecordTable.TABLE_NAME);
                List<DownloadRecord> result = new ArrayList<>();
                while (cursor.moveToNext()) {
                    result.add(Db.DownloadRecordTable.parseCursor(cursor));
                }
                subscriber.onNext(result);
                subscriber.onCompleted();
                cursor.close();
            }
        });
    }

    public boolean checkDownloadRecordNotExists(String url) {
        Cursor cursor = db.query("select " + Db.DownloadRecordTable.COLUMN_ID + " from "
                + Db.DownloadRecordTable.TABLE_NAME + " where url=?", url);
        return cursor.getCount() == 0;
    }

    public long insertDownloadRecord(String url) {
        return db.insert(Db.DownloadRecordTable.TABLE_NAME, Db.DownloadRecordTable.createRecordThroughUrl(url));
    }

    public long updateDownloadRecord(String url, DownloadStatus status) {
        return db.update(Db.DownloadRecordTable.TABLE_NAME, Db.DownloadRecordTable.createRecord(status), "url=?", url);
    }

    public int deleteDownloadRecord(String url) {
        return db.delete(Db.DownloadRecordTable.TABLE_NAME, "url=?", url);
    }
}