package zlc.season.rxdownload3.notification

import android.app.Notification
import android.content.Context
import android.support.v4.app.NotificationCompat
import zlc.season.rxdownload3.R
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.Status

class NotificationFactoryImpl : NotificationFactory {
    override fun build(context: Context, mission: Mission, status: Status): Notification {
        val builder = NotificationCompat.Builder(context, "")
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(mission.saveName)

        if (Status.isWaiting(status)) {
            builder.setContentText("等待中")
                    .setProgress(0, 0, true)
        }

        if (Status.isDownloading(status)) {
            builder.setContentText("下载中")
            if (status.chunkFlag) {
                builder.setProgress(0, 0, true)
            } else {
                builder.setProgress(status.totalSize.toInt(), status.downloadSize.toInt(), false)
            }
        }

        if (Status.isFailed(status)) {
            builder.setContentText("下载失败")
                    .setProgress(0, 0, false)
        }

        if (Status.isSucceed(status)) {
            builder.setContentText("下载成功")
                    .setProgress(status.totalSize.toInt(), status.downloadSize.toInt(), false)
        }

        if (Status.isSuspend(status)) {
            builder.setContentText("已暂停")
                    .setProgress(0, 0, false)
        }

        return builder.build()
    }
}