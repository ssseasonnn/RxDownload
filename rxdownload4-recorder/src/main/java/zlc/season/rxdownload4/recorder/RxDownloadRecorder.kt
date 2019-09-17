package zlc.season.rxdownload4.recorder

import android.annotation.SuppressLint
import io.reactivex.Flowable.fromIterable
import io.reactivex.Maybe
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers.io
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.notification.SimpleNotificationCreator
import zlc.season.rxdownload4.task.Task

@SuppressLint("CheckResult")
object RxDownloadRecorder {
    val taskDataBase by lazy { TaskDataBase.getInstance(clarityPotion) }

    fun getTaskList(page: Int, pageSize: Int): Maybe<List<TaskEntity>> {
        return taskDataBase.taskDao().page(page * pageSize, pageSize)
                .subscribeOn(io())
                .doOnSuccess { mapResult(it) }
    }

    fun getTaskListWithStatus(page: Int, pageSize: Int, vararg status: Status): Maybe<List<TaskEntity>> {
        return taskDataBase.taskDao().pageWithStatus(page * pageSize, pageSize, *status)
                .subscribeOn(io())
                .doOnSuccess { mapResult(it) }
    }

    fun getAllTask(): Maybe<List<TaskEntity>> {
        return taskDataBase.taskDao().getAll()
                .subscribeOn(io())
                .doOnSuccess { mapResult(it) }
    }

    fun getAllTaskWithStatus(vararg status: Status): Maybe<List<TaskEntity>> {
        return taskDataBase.taskDao().getAllWithStatus(*status)
                .subscribeOn(io())
                .doOnSuccess { mapResult(it) }
    }

    fun startAll() {
        getAllTaskWithStatus(Paused(), Failed())
                .flatMapPublisher { fromIterable(it) }
                .doOnNext {
                    it.task.createManager().start()
                }
                .subscribeBy()
    }


    fun stopAll() {
        getAllTaskWithStatus(Started(), Downloading())
                .flatMapPublisher { fromIterable(it) }
                .doOnNext {
                    it.task.createManager().stop()
                }
                .subscribeBy()
    }

    fun deleteAll() {
        getAllTask()
                .flatMapPublisher { fromIterable(it) }
                .doOnNext {
                    it.task.createManager().delete()
                }
                .subscribeBy()
    }

    private fun mapResult(list: List<TaskEntity>) {
        list.forEach {
            it.status.progress = it.progress
        }
    }

    private fun Task.createManager(): TaskManager {
        return manager(
                notificationCreator = SimpleNotificationCreator(),
                recorder = RoomRecorder()
        )
    }
}