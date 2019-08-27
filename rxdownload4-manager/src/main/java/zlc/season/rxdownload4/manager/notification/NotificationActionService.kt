package zlc.season.rxdownload4.manager.notification

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationCompat.Action
import android.support.v4.app.NotificationCompat.Action.Builder
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.utils.log

class NotificationActionService : IntentService("NotificationActionService") {
    companion object {
        const val INTENT_KEY = "task_url"

        val ACTION_START = "${clarityPotion.packageName}.rxdownload.action.START"
        val ACTION_STOP = "${clarityPotion.packageName}.rxdownload.action.STOP"
        val ACTION_CANCEL = "${clarityPotion.packageName}.rxdownload.action.CANCEL"

        fun startAction(url: String): Action {
            val intent = Intent(clarityPotion, NotificationActionService::class.java)
            intent.action = ACTION_START
            intent.putExtra(INTENT_KEY, url)

            val pendingIntent = PendingIntent.getService(clarityPotion, 0, intent, 0)

            return Builder(
                    R.drawable.ic_start,
                    clarityPotion.getString(R.string.action_start),
                    pendingIntent
            ).build()
        }

        fun stopAction(url: String): Action {
            val intent = Intent(clarityPotion, NotificationActionService::class.java)
            intent.action = ACTION_STOP
            intent.putExtra(INTENT_KEY, url)

            val pendingIntent = PendingIntent.getService(clarityPotion, 0, intent, 0)

            return Builder(
                    R.drawable.ic_pause,
                    clarityPotion.getString(R.string.action_stop),
                    pendingIntent
            ).build()
        }

        fun cancelAction(url: String): Action {
            val intent = Intent(clarityPotion, NotificationActionService::class.java)
            intent.action = ACTION_CANCEL
            intent.putExtra(INTENT_KEY, url)

            val pendingIntent = PendingIntent.getService(clarityPotion, 0, intent, 0)

            return Builder(
                    R.drawable.ic_close,
                    clarityPotion.getString(R.string.action_cancel),
                    pendingIntent
            ).build()
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.action.log()

        intent?.let {
            val url = it.getStringExtra(INTENT_KEY) ?: ""
            check(url.isNotEmpty()) { "Invalid url!" }

            val taskManager = url.manager()

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