package zlc.season.rxdownload4.recorder

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.task.Task

@Entity(
        tableName = TAB_NAME,
        indices = [Index("id", unique = true)]
)
class TaskEntity(
        @PrimaryKey
        var id: Int,

        @Embedded
        var task: Task,

        var status: Status,

        @Embedded
        var progress: Progress,

        //异常结束
        var abnormalExit: Boolean = false
)