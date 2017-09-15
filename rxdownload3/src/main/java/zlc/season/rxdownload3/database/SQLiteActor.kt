package zlc.season.rxdownload3.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.RealMission


class SQLiteActor(context: Context) : DbActor {
    private val DATABASE_NAME = "RxDownload.db"
    private val DATABASE_VERSION = 1

    private val sqLiteOpenHelper = object : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase?) {
            if (db == null) {
                return
            }

            db.beginTransaction()
            try {
                db.execSQL(CREATE)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        }
    }

    companion object {
        private val TABLE_NAME = "missions"

        private val TAG = "tag"
        private val URL = "url"
        private val SAVE_NAME = "save_name"
        private val SAVE_PATH = "save_path"
        private val RANGE_FLAG = "range_flag"

        private val CREATE = """
            CREATE TABLE $TABLE_NAME (
                $TAG TEXT PRIMARY KEY NOT NULL,
                $URL TEXT NOT NULL,
                $SAVE_NAME TEXT  NOT NULL,
                $SAVE_PATH TEXT  NOT NULL,
                $RANGE_FLAG INTEGER )
            """
    }


    override fun isExists(mission: RealMission): Boolean {
        val actual = mission.actual
        val readableDatabase = sqLiteOpenHelper.readableDatabase
        val cursor = readableDatabase.rawQuery(
                "SELECT $TAG FROM $TABLE_NAME where $TAG = ?",
                arrayOf(actual.tag)
        )
        cursor.use {
            cursor.moveToFirst()
            return cursor.count != 0
        }
    }

    override fun update(mission: RealMission) {
        val actual = mission.actual
        val writableDatabase = sqLiteOpenHelper.writableDatabase
        val cv = ContentValues()
        cv.put(URL, actual.url)
        cv.put(SAVE_NAME, actual.saveName)
        cv.put(SAVE_PATH, actual.savePath)
        cv.put(RANGE_FLAG, actual.rangeFlag)
        writableDatabase.update(TABLE_NAME, cv, "$TAG=?", arrayOf(actual.tag))
    }

    override fun create(mission: RealMission) {
        val actual = mission.actual
        val writableDatabase = sqLiteOpenHelper.writableDatabase
        val cv = ContentValues()
        cv.put(TAG, actual.tag)
        cv.put(URL, actual.url)
        cv.put(SAVE_NAME, actual.saveName)
        cv.put(SAVE_PATH, actual.savePath)
        cv.put(RANGE_FLAG, actual.rangeFlag)
        writableDatabase.insert(TABLE_NAME, null, cv)
    }

    override fun read(mission: RealMission) {
        val actual = mission.actual
        val readableDatabase = sqLiteOpenHelper.readableDatabase
        val cursor = readableDatabase.rawQuery(
                """
                    SELECT $TAG,$URL,$SAVE_NAME,$SAVE_PATH,$RANGE_FLAG
                    FROM $TABLE_NAME
                    where $TAG = ?
                    """,
                arrayOf(actual.tag)
        )

        cursor.use {
            cursor.moveToFirst()
            if (cursor.count == 0) {
                return
            }
            val saveName = cursor.getString(cursor.getColumnIndexOrThrow(SAVE_NAME))
            val savePath = cursor.getString(cursor.getColumnIndexOrThrow(SAVE_PATH))
            val rangeFlag = cursor.getInt(cursor.getColumnIndexOrThrow(RANGE_FLAG)) > 0

            actual.saveName = saveName
            actual.savePath = savePath
            actual.rangeFlag = rangeFlag
        }
    }

    override fun delete(mission: RealMission) {
        val actual = mission.actual
        val writableDatabase = sqLiteOpenHelper.writableDatabase
        writableDatabase.delete(TABLE_NAME, "$TAG=?", arrayOf(actual.tag))
    }

    override fun getAllMission(): Maybe<List<Mission>> {
        val readableDatabase = sqLiteOpenHelper.readableDatabase
        val cursor = readableDatabase.rawQuery(
                """
                    SELECT $TAG,$URL,$SAVE_NAME,$SAVE_PATH,$RANGE_FLAG
                    FROM $TABLE_NAME
                    """, null)

        cursor.use {
            val list = mutableListOf<Mission>()
            while (cursor.moveToNext()) {
                val tag = cursor.getString(cursor.getColumnIndexOrThrow(TAG))
                val url = cursor.getString(cursor.getColumnIndexOrThrow(URL))
                val saveName = cursor.getString(cursor.getColumnIndexOrThrow(SAVE_NAME))
                val savePath = cursor.getString(cursor.getColumnIndexOrThrow(SAVE_PATH))
                val rangeFlag = cursor.getInt(cursor.getColumnIndexOrThrow(RANGE_FLAG)) > 0
                list.add(Mission(url, saveName, savePath, rangeFlag, tag))
            }
            return Maybe.just(list)
        }
    }
}