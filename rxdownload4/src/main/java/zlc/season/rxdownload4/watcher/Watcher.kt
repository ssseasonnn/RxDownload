package zlc.season.rxdownload4.watcher

import zlc.season.rxdownload4.task.Task

interface Watcher {
    fun watch(task: Task)

    fun unwatch(task: Task)
}