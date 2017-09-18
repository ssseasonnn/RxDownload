package zlc.season.rxdownload3.notification

import android.app.Notification
import android.content.Context
import android.support.v4.app.NotificationCompat
import zlc.season.rxdownload3.R

class ForeServiceNotificationFactoryImpl : ForeServiceNotificationFactory {
    override fun build(context: Context): Notification {
        val builder = NotificationCompat.Builder(context, "")
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("后台任务正在下载中...")
        return builder.build()
    }
}