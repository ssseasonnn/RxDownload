package zlc.season.rxdownload4.recorder

import android.annotation.SuppressLint
import zlc.season.ironbranch.ioThread
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.manager.TaskRecorder
import zlc.season.rxdownload4.recorder.RxDownloadRecorder.taskDataBase
import zlc.season.rxdownload4.task.Task

@SuppressLint("CheckResult")
class RoomRecorder : TaskRecorder {
    override fun insert(task: Task) {
        ioThread {
            taskDataBase.taskDao().insert(task.map())
        }
    }

    override fun update(task: Task, status: Status) {
        ioThread {
            taskDataBase.taskDao().update(task.map(status))
        }

    }

    override fun delete(task: Task) {
        ioThread {
            taskDataBase.taskDao().delete(task.map())
        }
    }
}