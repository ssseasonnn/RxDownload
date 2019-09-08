package zlc.season.rxdownload4.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat.Action
import android.support.v4.app.NotificationCompat.Builder
import android.support.v4.app.NotificationManagerCompat
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.task.Task


private val notificationManager by lazy {
    clarityPotion.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}

fun cancelNotification(task: Task) {
    notificationManager.cancel(task.hashCode())
}

fun isEnableNotification(): Boolean {
    val notificationManagerCompat = NotificationManagerCompat.from(clarityPotion)
    return notificationManagerCompat.areNotificationsEnabled()
}

fun createNotificationChannel(
        channelId: String,
        channelName: String,
        channelDescription: String
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
        )
        notificationChannel.description = channelDescription

        notificationChannel.enableVibration(false)
        notificationChannel.enableLights(false)

        notificationManager.createNotificationChannel(notificationChannel)
    }
}


fun createNotificationBuilder(
        channelId: String,
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
            .setVibrate(longArrayOf(0L))
            .setDefaults(Notification.DEFAULT_ALL)

    progress?.let {
        notificationBuilder.setProgress(
                it.totalSize.toInt(),
                it.downloadSize.toInt(),
                it.isChunked
        )
    }

    actions.forEach {
        notificationBuilder.addAction(it)
    }

    return notificationBuilder
}
