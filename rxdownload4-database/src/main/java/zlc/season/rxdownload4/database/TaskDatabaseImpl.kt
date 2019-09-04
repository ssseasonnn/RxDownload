package zlc.season.rxdownload4.database

import io.reactivex.Flowable
import zlc.season.claritypotion.ClarityPotion
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.manager.TaskDatabase
import zlc.season.rxdownload4.task.Task

class TaskDatabaseImpl : TaskDatabase {

    private val taskDataBase = TaskDataBase.getInstance(ClarityPotion.clarityPotion)

    override fun contain(task: Task): Boolean {
//        val taskEntity = taskDataBase.taskDao().get(task.hashCode())
        return false
    }

    override fun getAll(): Flowable<List<Task>> {
        return Flowable.just(emptyList())
    }

    override fun insert(task: Task) {
        taskDataBase.taskDao().insert(task.map())
    }

    override fun update(task: Task, status: Status) {
        taskDataBase.taskDao().update(task.map(status))
    }

    override fun delete(task: Task) {
    }
}