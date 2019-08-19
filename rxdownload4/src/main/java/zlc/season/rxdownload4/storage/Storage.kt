package zlc.season.rxdownload4.storage

import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.task.TaskInfo

interface Storage {
    fun load(task: Task): TaskInfo

    fun save(task: Task)
}