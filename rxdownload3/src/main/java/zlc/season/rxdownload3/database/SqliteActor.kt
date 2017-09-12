package zlc.season.rxdownload3.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission


class SqliteActor(context: Context) : DbActor {
    override fun isExists(mission: Mission): Boolean {
        val readableDatabase = sqliteOpenHelper.readableDatabase
        val cursor = readableDatabase.rawQuery(
                "SELECT ${Table.TAG} FROM ${Table.TABLE_NAME} where ${Table.TAG} = ?",
                arrayOf(mission.tag)
        )
        cursor.use {
            cursor.moveToFirst()
            return cursor.count != 0
        }
    }

    val DATABASE_NAME = "RxDownload.db"
    val DATABASE_VERSION = 1

    val sqliteOpenHelper = object : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
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

    override fun update(mission: Mission) {
        val writableDatabase = sqliteOpenHelper.writableDatabase
        val cv = ContentValues()
        cv.put(Table.URL, mission.url)
        cv.put(Table.SAVE_NAME, mission.saveName)
        cv.put(Table.SAVE_PATH, mission.savePath)
        cv.put(Table.RANGE_FLAG, mission.rangeFlag)
        writableDatabase.update(Table.TABLE_NAME, cv, "${Table.TAG}=?", arrayOf(mission.tag))
    }

    override fun create(mission: Mission) {
        val writableDatabase = sqliteOpenHelper.writableDatabase
        val cv = ContentValues()
        cv.put(Table.TAG, mission.tag)
        cv.put(Table.URL, mission.url)
        cv.put(Table.SAVE_NAME, mission.saveName)
        cv.put(Table.SAVE_PATH, mission.savePath)
        cv.put(Table.RANGE_FLAG, mission.rangeFlag)
        writableDatabase.insert(Table.TABLE_NAME, null, cv)
    }

    override fun read(mission: Mission) {
        val readableDatabase = sqliteOpenHelper.readableDatabase
        val cursor = readableDatabase.rawQuery(
                """
                    SELECT ${Table.TAG},${Table.URL},${Table.SAVE_NAME},${Table.SAVE_PATH},${Table.RANGE_FLAG}
                    FROM ${Table.TABLE_NAME}
                    where ${Table.TAG} = ?
                    """,
                arrayOf(mission.tag)
        )

        cursor.use {
            cursor.moveToFirst()
            if (cursor.count == 0) {
                return
            }
            val saveName = cursor.getString(cursor.getColumnIndexOrThrow(Table.SAVE_NAME))
            val savePath = cursor.getString(cursor.getColumnIndexOrThrow(Table.SAVE_PATH))
            val rangeFlag = cursor.getInt(cursor.getColumnIndexOrThrow(Table.RANGE_FLAG)) > 0

            mission.saveName = saveName
            mission.savePath = savePath
            mission.rangeFlag = rangeFlag
        }
    }

    override fun delete(mission: Mission) {
        val writableDatabase = sqliteOpenHelper.writableDatabase
        writableDatabase.delete(Table.TABLE_NAME, "${Table.TAG}=?", arrayOf(mission.tag))
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
                $RANGE_FLAG INTEGER,
                $STATUS INTEGER )
            """
}
