package zlc.season.rxdownload4.database

import zlc.season.rxdownload4.manager.Normal
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.task.Task

fun Task.map(status: Status = Normal()): TaskEntity {
    return TaskEntity(
            id = hashCode(),
            url = url,
            taskName = taskName,
            saveName = saveName,
            savePath = savePath,
            status = TaskEntity.mapStatus(status),
            downloadSize = status.progress.downloadSize,
            totalSize = status.progress.totalSize,
            isChunked = status.progress.isChunked
    )
}


fun TaskEntity.map(): Task {
    return Task(url, taskName, saveName, savePath)
}