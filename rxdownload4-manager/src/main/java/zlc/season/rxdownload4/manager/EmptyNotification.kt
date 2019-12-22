package zlc.season.rxdownload4.manager

import android.app.Notification
import zlc.season.rxdownload4.task.Task

object EmptyNotification : NotificationCreator {
    override fun init(task: Task) {

    }

    override fun create(task: Task, status: Status): Notification? {
        return null
    }
}