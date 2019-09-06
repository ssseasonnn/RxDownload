package zlc.season.rxdownload4.notification

import android.app.IntentService
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.support.v4.app.NotificationCompat.Action
import android.support.v4.app.NotificationCompat.Action.Builder
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.manager.delete
import zlc.season.rxdownload4.manager.manager
import zlc.season.rxdownload4.manager.start
import zlc.season.rxdownload4.manager.stop
import zlc.season.rxdownload4.task.Task

class NotificationActionService : IntentService("NotificationActionService") {
    companion object {
        const val INTENT_KEY = "task_url"

        val ACTION_START = "${clarityPotion.packageName}.rxdownload.action.START"
        val ACTION_STOP = "${clarityPotion.packageName}.rxdownload.action.STOP"
        val ACTION_CANCEL = "${clarityPotion.packageName}.rxdownload.action.CANCEL"

        fun startAction(task: Task): Action {
            return Builder(
                    R.drawable.ic_start,
                    clarityPotion.getString(R.string.action_start),
                    createPendingIntent(ACTION_START, task)
            ).build()
        }

        fun stopAction(task: Task): Action {
            return Builder(
                    R.drawable.ic_pause,
                    clarityPotion.getString(R.string.action_stop),
                    createPendingIntent(ACTION_STOP, task)
            ).build()
        }

        fun cancelAction(task: Task): Action {
            return Builder(
                    R.drawable.ic_cancel,
                    clarityPotion.getString(R.string.action_cancel),
                    createPendingIntent(ACTION_CANCEL, task)
            ).build()
        }

        private fun createPendingIntent(action: String, task: Task): PendingIntent {
            val intent = Intent(clarityPotion, NotificationActionService::class.java)
            intent.action = action
            intent.putExtra(INTENT_KEY, task.url)

            return PendingIntent.getService(clarityPotion, task.hashCode(), intent, FLAG_UPDATE_CURRENT)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            val url = it.getStringExtra(INTENT_KEY) ?: ""
            check(url.isNotEmpty()) { "Invalid url!" }

            val taskManager = url.manager(notificationCreator = SimpleNotificationCreator())

            when (it.action) {
                ACTION_START -> {
                    taskManager.start()
                }
                ACTION_STOP -> {
                    taskManager.stop()
                }
                ACTION_CANCEL -> {
                    taskManager.delete()
                }
            }
        }
    }
}