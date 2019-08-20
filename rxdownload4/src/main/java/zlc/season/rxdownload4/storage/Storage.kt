package zlc.season.rxdownload4.storage

import zlc.season.rxdownload4.task.Task

interface Storage {
    fun load(task: Task)

    fun save(task: Task)

    fun delete(task: Task)
}