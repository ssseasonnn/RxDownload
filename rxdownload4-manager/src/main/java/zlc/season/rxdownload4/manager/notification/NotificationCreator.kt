package zlc.season.rxdownload4.manager.notification

import android.app.Notification
import zlc.season.rxdownload4.manager.Status
import zlc.season.rxdownload4.task.Task

interface NotificationCreator {
    fun init(task: Task)

    fun create(task: Task, status: Status): Notification?
}