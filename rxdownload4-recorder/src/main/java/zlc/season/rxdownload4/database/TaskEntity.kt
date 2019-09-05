package zlc.season.rxdownload4.database

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.task.Task

@Entity(
        tableName = "task_record",
        indices = [Index("id", unique = true)]
)
class TaskEntity(
        @PrimaryKey
        val id: Int,

        @Embedded
        val task: Task,

        val status: Status,

        @Embedded
        val progress: Progress,

        val time: Long = System.currentTimeMillis()
)