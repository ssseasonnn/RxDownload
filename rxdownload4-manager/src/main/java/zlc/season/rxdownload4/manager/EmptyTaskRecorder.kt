package zlc.season.rxdownload4.manager

import io.reactivex.Flowable
import zlc.season.rxdownload4.task.Task

class EmptyTaskRecorder : TaskRecorder {
    override fun getAllWithStatus(status: Status): Flowable<List<Task>> {
        return Flowable.empty()
    }

    override fun pageWithStatus(page: Int, pageSize: Int, status: Status): Flowable<List<Task>> {
        return Flowable.empty()
    }

    override fun insert(task: Task) {

    }

    override fun update(task: Task, status: Status) {
    }

    override fun delete(task: Task) {
    }

    override fun getAll(): Flowable<List<Task>> {
        return Flowable.empty()
    }

    override fun page(page: Int, pageSize: Int): Flowable<List<Task>> {
        return Flowable.empty()
    }
}