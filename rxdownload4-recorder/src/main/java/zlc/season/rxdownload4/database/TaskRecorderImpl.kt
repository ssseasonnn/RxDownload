package zlc.season.rxdownload4.database

import android.annotation.SuppressLint
import io.reactivex.Flowable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers.io
import zlc.season.claritypotion.ClarityPotion
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.manager.TaskRecorder
import zlc.season.rxdownload4.task.Task

@SuppressLint("CheckResult")
class TaskRecorderImpl : TaskRecorder {


    private val taskDataBase = TaskDataBase.getInstance(ClarityPotion.clarityPotion)

    private val taskDao = taskDataBase.taskDao()

    override fun page(page: Int, pageSize: Int): Flowable<List<Task>> {
        return taskDao.page(page * pageSize, pageSize)
                .subscribeOn(io())
                .map { mapResult(it) }
    }

    override fun getAll(): Flowable<List<Task>> {
        return taskDao.getAll()
                .subscribeOn(io())
                .map { mapResult(it) }
    }

    private fun mapResult(list: List<TaskEntity>): MutableList<Task> {
        val result = mutableListOf<Task>()
        list.mapTo(result) { it.task }
        return result
    }

    override fun getAllWithStatus(status: Status): Flowable<List<Task>> {
        return taskDao.getAllWithStatus(status)
                .subscribeOn(io())
                .map { mapResult(it) }
    }

    override fun pageWithStatus(page: Int, pageSize: Int, status: Status): Flowable<List<Task>> {
        return taskDao.pageWithStatus(page * pageSize, pageSize, status)
                .subscribeOn(io())
                .map { mapResult(it) }
    }

    override fun insert(task: Task) {
        taskDao.insert(task.map())
                .subscribeOn(io())
                .subscribeBy()
    }

    override fun update(task: Task, status: Status) {
        taskDao.update(task.map(status))
                .subscribeOn(io())
                .subscribeBy()
    }

    override fun delete(task: Task) {
        taskDao.delete(task.map())
                .subscribeOn(io())
                .subscribeBy()
    }
}