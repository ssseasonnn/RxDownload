package zlc.season.rxdownload4.manager

import io.reactivex.Flowable
import zlc.season.rxdownload4.task.Task

interface TaskRecorder {
    fun insert(task: Task)

    fun update(task: Task, status: Status)

    fun delete(task: Task)

    fun getAll(): Flowable<List<Task>>

    fun getAllWithStatus(status: Status): Flowable<List<Task>>

    fun page(page: Int, pageSize: Int): Flowable<List<Task>>

    fun pageWithStatus(page: Int, pageSize: Int, status: Status): Flowable<List<Task>>

}