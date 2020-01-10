package zlc.season.rxdownload4.recorder

import android.annotation.SuppressLint
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers.io
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.manager.TaskRecorder
import zlc.season.rxdownload4.recorder.RxDownloadRecorder.taskDataBase
import zlc.season.rxdownload4.task.Task

@SuppressLint("CheckResult")
class RoomRecorder : TaskRecorder {
    override fun insert(task: Task) {
        taskDataBase.taskDao().insert(task.map()).subscribeOn(io()).subscribeBy { }
    }

    override fun update(task: Task, status: Status) {
        taskDataBase.taskDao().update(task.map(status)).subscribeOn(io()).subscribeBy { }
    }

    override fun delete(task: Task) {
        taskDataBase.taskDao().delete(task.map()).subscribeOn(io()).subscribeBy { }
    }
}