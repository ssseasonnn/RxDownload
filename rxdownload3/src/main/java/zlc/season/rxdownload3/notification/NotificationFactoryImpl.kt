package zlc.season.rxdownload3.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.support.v4.app.NotificationCompat
import zlc.season.rxdownload3.R
import zlc.season.rxdownload3.core.*


class NotificationFactoryImpl : NotificationFactory {
    override fun build(context: Context, mission: Mission, status: Status): Notification {
        val channelId = "RxDownload"
        val channelName = "RxDownload"

        if (SDK_INT >= O) {
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            channel.enableLights(true)
            channel.setShowBadge(true)
            channel.lightColor = Color.GREEN
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(mission.saveName)

        if (status is Waiting) {
            builder.setContentText("等待中")
                    .setProgress(0, 0, true)
        }

        if (status is Downloading) {
            builder.setContentText("下载中")
            if (status.chunkFlag) {
                builder.setProgress(0, 0, true)
            } else {
                builder.setProgress(status.totalSize.toInt(), status.downloadSize.toInt(), false)
            }
        }

        if (status is Failed) {
            builder.setContentText("下载失败")
                    .setProgress(0, 0, false)
        }

        if (status is Succeed) {
            builder.setContentText("下载成功")
                    .setProgress(0, 0, false)
        }

        if (status is Suspend) {
            builder.setContentText("已暂停")
                    .setProgress(0, 0, false)
        }

        return builder.build()
    }
}