package zlc.season.rxdownload.kotlin_demo

import android.content.ContentValues
import android.content.Context
import zlc.season.rxdownload3.core.RealMission
import zlc.season.rxdownload3.database.SQLiteActor


class CustomSqliteActor(context: Context) : SQLiteActor(context) {
    private val IMG = "img"

    override val CREATE = """
            CREATE TABLE $TABLE_NAME (
                $TAG TEXT PRIMARY KEY NOT NULL,
                $URL TEXT NOT NULL,
                $SAVE_NAME TEXT,
                $SAVE_PATH TEXT,
                $RANGE_FLAG INTEGER,
                $TOTAL_SIZE TEXT,
                $IMG TEXT)
            """

    override fun create(mission: RealMission) {
        super.create(mission)
    }

    override fun onUpdate(mission: RealMission): ContentValues {
        return super.onUpdate(mission)
    }
}