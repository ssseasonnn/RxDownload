package zlc.season.rxdownload4.manager.notification

import android.app.Notification
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat.Action
import android.support.v4.app.NotificationCompat.Builder
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.manager.notification.NotificationActionService.Companion.cancelAction
import zlc.season.rxdownload4.manager.notification.NotificationActionService.Companion.startAction
import zlc.season.rxdownload4.manager.notification.NotificationActionService.Companion.stopAction
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

    private fun createNotificationBuilder(
            title: String,
            content: String,
            icon: Int,
            intent: PendingIntent? = null,
            progress: Progress? = null,
            actions: List<Action> = emptyList()
    ): Builder {
        val notificationBuilder = Builder(clarityPotion, channelId)
        notificationBuilder.setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(icon)
                .setContentIntent(intent)

        progress?.let {
            notificationBuilder.setProgress(it.totalSize.toInt(), it.downloadSize.toInt(), it.isChunked)
        }


        actions.forEach {
            notificationBuilder.addAction(it)
        }

        return notificationBuilder
    }

    private fun initBuilder(task: Task) {
        startedBuilder = createNotificationBuilder(
                title = task.taskName,
                content = "下载中...",
                icon = R.drawable.ic_download,
                actions = listOf(stopAction(task), cancelAction(task))
        )

        downloadingBuilder = createNotificationBuilder(
                title = task.taskName,
                content = "",
                icon = R.drawable.ic_download,
                progress = null,
                actions = listOf(stopAction(task), cancelAction(task))
        )

        pausedBuilder = createNotificationBuilder(
                title = task.taskName,
                content = "已暂停下载",
                icon = R.drawable.ic_pause,
                actions = listOf(startAction(task), cancelAction(task))
        )

        completedBuilder = createNotificationBuilder(
                title = task.taskName,
                content = "下载完成",
                icon = R.drawable.ic_completed
        )

        failedBuilder = createNotificationBuilder(
                title = task.taskName,
                content = "下载错误",
                icon = R.drawable.ic_pause,
                actions = listOf(startAction(task), cancelAction(task))
        )
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
        }
    }
}