package zlc.season.rxdownload3.notification

import android.app.Notification
import android.content.Context
import zlc.season.rxdownload3.core.RealMission
import zlc.season.rxdownload3.core.Status


interface NotificationFactory {
    fun build(context: Context, mission: RealMission, status: Status): Notification?
}

