package zlc.season.rxdownload4.recorder

import zlc.season.rxdownload4.manager.Normal
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.task.Task

fun Task.map(status: Status = Normal()): TaskEntity {
    return TaskEntity(
            id = hashCode(),
            task = this,
            status = status,
            progress = status.progress
    )
}
