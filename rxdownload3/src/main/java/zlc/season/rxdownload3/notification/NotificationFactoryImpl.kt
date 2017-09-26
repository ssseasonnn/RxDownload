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
import zlc.season.rxdownload3.extension.ApkInstallExtension


class NotificationFactoryImpl : NotificationFactory {
    override fun build(context: Context, mission: Mission, status: Status): Notification {
        val channelId = "RxDownload"
        val channelName = "RxDownload"

        createOreoChannel(context, channelId, channelName)

        val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(mission.saveName)

        if (status is Waiting) {
            builder.setContentText("等待中")
            showIndeterminateProgress(builder)
        }

        if (status is Downloading) {
            builder.setContentText("下载中")
            if (status.chunkFlag) {
                showIndeterminateProgress(builder)
            } else {
                showProgress(builder, status.totalSize.toInt(), status.downloadSize.toInt())
            }
        }

        if (status is Failed) {
            builder.setContentText("下载失败")
            dismissProgress(builder)
        }

        if (status is Succeed) {
            builder.setContentText("下载成功")
            dismissProgress(builder)
        }

        if (status is Suspend) {
            builder.setContentText("已暂停")
            dismissProgress(builder)
        }

        if (status is ApkInstallExtension.Installing) {
            builder.setContentText("安装中")
            dismissProgress(builder)
        }

        if (status is ApkInstallExtension.Installed) {
            builder.setContentText("安装完成")
            dismissProgress(builder)
        }

        return builder.build()
    }

    private fun showProgress(builder: NotificationCompat.Builder, max: Int, progress: Int) {
        builder.setProgress(max, progress, false)
    }

    private fun showIndeterminateProgress(builder: NotificationCompat.Builder) {
        builder.setProgress(0, 0, true)
    }

    private fun dismissProgress(builder: NotificationCompat.Builder) {
        builder.setProgress(0, 0, false)
    }

    private fun createOreoChannel(context: Context, channelId: String, channelName: String) {
        if (SDK_INT >= O) {
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            channel.enableLights(true)
            channel.setShowBadge(true)
            channel.lightColor = Color.GREEN
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            notificationManager.createNotificationChannel(channel)
        }
    }
}