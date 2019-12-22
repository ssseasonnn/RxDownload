package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.task.Task

object EmptyRecorder : TaskRecorder {

    override fun insert(task: Task) {
    }

    override fun update(task: Task, status: Status) {
    }

    override fun delete(task: Task) {
    }
}