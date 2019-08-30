package zlc.season.rxdownload4.manager

import android.app.Notification
import zlc.season.rxdownload4.task.Task

interface NotificationCreator {
    fun init(task: Task)

    fun create(task: Task, status: Status): Notification?
}