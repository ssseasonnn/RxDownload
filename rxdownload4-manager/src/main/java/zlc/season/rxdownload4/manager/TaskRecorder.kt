package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.task.Task

interface TaskRecorder {
    fun insert(task: Task)

    fun update(task: Task, status: Status)

    fun delete(task: Task)
}