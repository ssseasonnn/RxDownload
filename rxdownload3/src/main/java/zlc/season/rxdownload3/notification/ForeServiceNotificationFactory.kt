package zlc.season.rxdownload3.notification

import android.app.Notification
import android.content.Context

interface ForeServiceNotificationFactory {
    fun build(context: Context): Notification
}