package zlc.season.rxdownload4.manager

import io.reactivex.Flowable
import zlc.season.rxdownload4.task.Task

interface TaskDatabase {
    fun contain(task: Task): Boolean

    fun insert(task: Task)

    fun update(task: Task, status: Status)

    fun delete(task: Task)

    fun getAll(): Flowable<List<Task>>

}