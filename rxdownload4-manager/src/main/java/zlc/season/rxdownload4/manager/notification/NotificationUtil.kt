package zlc.season.rxdownload4.manager.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.manager.R

val notificationManager by lazy {
    clarityPotion.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}

fun isEnableNotification(): Boolean {
    val notificationManagerCompat = NotificationManagerCompat.from(clarityPotion)
    return notificationManagerCompat.areNotificationsEnabled()
}

fun createNotificationChannel(
        channelId: String,
        channelName: String,
        channelDescription: String
): String {

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.description = channelDescription
        notificationManager.createNotificationChannel(notificationChannel)

        channelId
    } else {
        // Returns null for pre-O (26) devices.
        ""
    }
}

