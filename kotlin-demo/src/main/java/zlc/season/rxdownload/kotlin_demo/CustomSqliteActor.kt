package zlc.season.rxdownload.kotlin_demo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.RealMission
import zlc.season.rxdownload3.core.Status
import zlc.season.rxdownload3.database.SQLiteActor


class CustomSqliteActor(context: Context) : SQLiteActor(context) {
    private val IMG = "img"

    override fun provideCreateSql(): String {
        return """
            CREATE TABLE $TABLE_NAME (
                $TAG TEXT PRIMARY KEY NOT NULL,
                $URL TEXT NOT NULL,
                $SAVE_NAME TEXT,
                $SAVE_PATH TEXT,
                $RANGE_FLAG INTEGER,
                $CURRENT_SIZE TEXT,
                $TOTAL_SIZE TEXT,
                $STATUS_FLAG INTEGER,
                $IMG TEXT)
            """
    }

    override fun onCreate(mission: RealMission): ContentValues {
        val cv = super.onCreate(mission)
        if (mission.actual is CustomMission) {
            val actualMission = mission.actual as CustomMission
            cv.put(IMG, actualMission.img)
        }

        return cv
    }

    override fun onGetAllMission(cursor: Cursor): Mission {
        val mission = super.onGetAllMission(cursor)
        var img = cursor.getString(cursor.getColumnIndexOrThrow(IMG))
        if (img.isNullOrEmpty()) {
            img = "no img"
        }
        return CustomMission(mission, img)
    }
}