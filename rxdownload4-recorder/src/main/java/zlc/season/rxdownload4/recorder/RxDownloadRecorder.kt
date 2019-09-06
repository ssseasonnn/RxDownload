package zlc.season.rxdownload4.recorder

import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers.io
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.manager.Status

object RxDownloadRecorder {
    val taskDataBase by lazy { TaskDataBase.getInstance(clarityPotion) }

    fun getTaskList(page: Int, pageSize: Int): Maybe<List<TaskEntity>> {
        return taskDataBase.taskDao().page(page * pageSize, pageSize)
                .subscribeOn(io())
                .doOnSuccess { mapResult(it) }
    }

    fun getTaskListWithStatus(page: Int, pageSize: Int, status: Status): Maybe<List<TaskEntity>> {
        return taskDataBase.taskDao().pageWithStatus(page * pageSize, pageSize, status)
                .subscribeOn(io())
                .doOnSuccess { mapResult(it) }
    }

    fun getAllTask(): Maybe<List<TaskEntity>> {
        return taskDataBase.taskDao().getAll()
                .subscribeOn(io())
                .doOnSuccess { mapResult(it) }
    }

    fun getAllTaskWithStatus(status: Status): Maybe<List<TaskEntity>> {
        return taskDataBase.taskDao().getAllWithStatus(status)
                .subscribeOn(io())
                .doOnSuccess { mapResult(it) }
    }

    private fun mapResult(list: List<TaskEntity>) {
        list.forEach {
            it.status.progress = it.progress
        }
    }
}