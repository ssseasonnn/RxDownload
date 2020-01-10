package zlc.season.rxdownload4.recorder

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import zlc.season.rxdownload4.manager.Normal
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.recorder.StatusConverter.Companion.DOWNLOADING
import zlc.season.rxdownload4.recorder.StatusConverter.Companion.PAUSED
import zlc.season.rxdownload4.recorder.StatusConverter.Companion.STARTED
import zlc.season.rxdownload4.task.Task

internal const val DB_NAME = "TaskRecord.db"
internal const val TAB_NAME = "task_record"

internal fun fixAbnormalState(db: SupportSQLiteDatabase) {
    db.beginTransaction()
    try {
        db.execSQL("""UPDATE $TAB_NAME SET status = $PAUSED, abnormalExit = "1" WHERE status = $STARTED""")
        db.execSQL("""UPDATE $TAB_NAME SET status = $PAUSED, abnormalExit = "1" WHERE status = $DOWNLOADING""")
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

/**
 * Migration from v1 to v2.
 *
 * Add extraInfo column
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""ALTER TABLE $TAB_NAME ADD COLUMN extraInfo TEXT NOT NULL DEFAULT "" """)
    }
}

internal fun Task.map(status: Status = Normal()): TaskEntity {
    return TaskEntity(
            id = hashCode(),
            task = this,
            status = status,
            progress = status.progress
    )
}