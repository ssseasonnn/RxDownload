package zlc.season.rxdownload3.notification

import android.app.Notification
import android.content.Context
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.Status


interface NotificationFactory {
    fun build(context: Context, mission: Mission, status: Status): Notification
}

