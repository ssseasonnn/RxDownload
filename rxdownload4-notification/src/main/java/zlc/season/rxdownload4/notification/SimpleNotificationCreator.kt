package zlc.season.rxdownload4.notification

import android.app.Notification
import android.support.v4.app.NotificationCompat.Builder
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.notification.NotificationActionService.Companion.cancelAction
import zlc.season.rxdownload4.notification.NotificationActionService.Companion.startAction
import zlc.season.rxdownload4.notification.NotificationActionService.Companion.stopAction
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.log

class SimpleNotificationCreator : NotificationCreator {
    private val channelId = "RxDownload"
    private val channelName = "RxDownload"
    private val channelDesc = "RxDownload"

    private var startedBuilder: Builder? = null
    private var downloadingBuilder: Builder? = null
    private var pausedBuilder: Builder? = null
    private var completedBuilder: Builder? = null
    private var failedBuilder: Builder? = null

    override fun init(task: Task) {
        if (!isEnableNotification()) {
            "Notification not enable".log()
        }

        createNotificationChannel(channelId, channelName, channelDesc)

        initBuilder(task)
    }

    override fun create(task: Task, status: Status): Notification? {
        return when (status) {
            is Normal -> null
            is Started -> startedBuilder?.build()
            is Downloading ->
                downloadingBuilder?.setProgress(
                        status.progress.totalSize.toInt(),
                        status.progress.downloadSize.toInt(),
                        status.progress.isChunked
                )?.build()
            is Paused -> pausedBuilder?.build()
            is Completed -> completedBuilder?.build()
            is Failed -> failedBuilder?.build()
            is Deleted -> null
            else -> null
        }
    }

    private fun initBuilder(task: Task) {
        startedBuilder = createNotificationBuilder(
                channelId = channelId,
                title = task.taskName,
                content = clarityPotion.getString(R.string.notification_started_text),
                icon = R.drawable.ic_download,
                actions = listOf(stopAction(task), cancelAction(task))
        )

        downloadingBuilder = createNotificationBuilder(
                channelId = channelId,
                title = task.taskName,
                content = "",
                icon = R.drawable.ic_download,
                progress = null,
                actions = listOf(stopAction(task), cancelAction(task))
        )

        pausedBuilder = createNotificationBuilder(
                channelId = channelId,
                title = task.taskName,
                content = clarityPotion.getString(R.string.notification_paused_text),
                icon = R.drawable.ic_pause,
                actions = listOf(startAction(task), cancelAction(task))
        )

        completedBuilder = createNotificationBuilder(
                channelId = channelId,
                title = task.taskName,
                content = clarityPotion.getString(R.string.notification_completed_text),
                icon = R.drawable.ic_completed
        )

        failedBuilder = createNotificationBuilder(
                channelId = channelId,
                title = task.taskName,
                content = clarityPotion.getString(R.string.notification_failed_text),
                icon = R.drawable.ic_pause,
                actions = listOf(startAction(task), cancelAction(task))
        )
    }
}