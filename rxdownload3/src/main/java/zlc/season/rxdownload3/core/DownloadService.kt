package zlc.season.rxdownload3.core

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import zlc.season.rxdownload3.R
import java.io.File


class DownloadService : Service() {
    private val missionBox = LocalMissionBox()
    private val binder = DownloadBinder()
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, "")
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("任务下载中")
        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        missionBox.stopAll()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    inner class DownloadBinder : Binder() {
        fun create(callback: StatusCallback, mission: Mission) {
            missionBox.create(mission).subscribe({
                callback.apply(it)
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

        fun getFile(fileCallback: FileCallback, mission: Mission) {
            missionBox.getFile(mission).subscribe({
                fileCallback.apply(it)
            })
        }
    }


    interface StatusCallback {
        fun apply(status: Status)
    }

    interface FileCallback {
        fun apply(file: File)
    }
}