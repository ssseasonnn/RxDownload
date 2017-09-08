package zlc.season.rxdownload3.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission


class SqliteActor(context: Context) : DbActor {
    val DATABASE_NAME = "RxDownload.db"
    val DATABASE_VERSION = 1

    val sqliteOpenHelper: SQLiteOpenHelper = SqliteOpenHelper(context, DATABASE_NAME, DATABASE_VERSION)


    override fun update(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun create(mission: Mission) {
        val writableDatabase = sqliteOpenHelper.writableDatabase
        
    }

    override fun read(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    init {

    }

    override fun getAllMission(): Maybe<List<Mission>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object Table {
    val TABLE_NAME = "missions"
    val ID = "id"
    val TAG = "tag"
    val URL = "url"
    val SAVE_NAME = "save_name"
    val SAVE_PATH = "save_path"
    val RANGE_FLAG = "range_flag"
    val STATUS = "status"

    val CREATE = """
            CREATE TABLE $TABLE_NAME (
                $TAG TEXT PRIMARY KEY NOT NULL,
                $URL TEXT NOT NULL,
                $SAVE_NAME TEXT  NOT NULL,
                $SAVE_PATH TEXT  NOT NULL,
                $RANGE_FLAG INTEGER NOT NULL,
                $STATUS INTEGER NOT NULL )
            """
}

class SqliteOpenHelper(context: Context, name: String, version: Int)
    : SQLiteOpenHelper(context, name, null, version) {
    override fun onCreate(db: SQLiteDatabase?) {
        if (db == null) {
            return
        }

        db.beginTransaction()
        try {
            db.execSQL(Table.CREATE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

}