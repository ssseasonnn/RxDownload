package zlc.season.rxdownload3.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension


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
        private val TOTAL_SIZE = "total_size"
        private val STATUS = "status"

        private val CREATE = """
            CREATE TABLE $TABLE_NAME (
                $TAG TEXT PRIMARY KEY NOT NULL,
                $URL TEXT NOT NULL,
                $SAVE_NAME TEXT,
                $SAVE_PATH TEXT,
                $RANGE_FLAG INTEGER,
                $TOTAL_SIZE TEXT,
                $STATUS TEXT )
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
        cv.put(SAVE_NAME, actual.saveName)
        cv.put(SAVE_PATH, actual.savePath)
        cv.put(TOTAL_SIZE, mission.totalSize)
        cv.put(STATUS, convertStatus(mission.status))
        writableDatabase.update(TABLE_NAME, cv, "$TAG=?", arrayOf(actual.tag))
    }

    private fun convertStatus(status: Status): Int {
        return when (status) {
            is Suspend -> 1
            is Waiting -> 2
            is Downloading -> 3
            is Failed -> 4
            is Succeed -> 5
            is ApkInstallExtension.Installing -> 6
            is ApkInstallExtension.Installed -> 7
            else -> -1
        }
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
        cv.put(TOTAL_SIZE, mission.totalSize)
        writableDatabase.insert(TABLE_NAME, null, cv)
    }

    override fun read(mission: RealMission) {
        val actual = mission.actual
        val readableDatabase = sqLiteOpenHelper.readableDatabase
        val cursor = readableDatabase.rawQuery(
                """
                    SELECT $TAG,$URL,$SAVE_NAME,$SAVE_PATH,$RANGE_FLAG,$TOTAL_SIZE
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
            val totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(TOTAL_SIZE))

            actual.saveName = saveName
            actual.savePath = savePath
            actual.rangeFlag = rangeFlag
            mission.totalSize = totalSize
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
                    SELECT $TAG,$URL,$SAVE_NAME,$SAVE_PATH,$RANGE_FLAG,$TOTAL_SIZE
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
                val totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(TOTAL_SIZE))
                list.add(Mission(url, saveName, savePath, rangeFlag, tag))
            }
            return Maybe.just(list)
        }
    }
}