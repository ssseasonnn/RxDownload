package zlc.season.rxdownload3.core

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder


class DownloadService : Service() {
    private val missionBox = LocalMissionBox()
    private val binder = DownloadBinder()
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    inner class DownloadBinder : Binder() {
        fun create(callback: BinderCallback, mission: Mission) {
            missionBox.create(mission).subscribe({
                callback.onUpdate(it)
            })
        }

        fun start(mission: Mission) {
            missionBox.start(mission).subscribe()
        }

        fun stop(mission: Mission) {
            missionBox.stop(mission).subscribe()
        }

        fun startAll() {
            missionBox.startAll().subscribe()
        }

        fun stopAll() {
            missionBox.stopAll().subscribe()
        }
    }


    interface BinderCallback {
        fun onUpdate(status: Status)
    }

}