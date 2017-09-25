package zlc.season.rxdownload3.core

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import zlc.season.rxdownload3.extension.Extension
import zlc.season.rxdownload3.helper.logd
import zlc.season.rxdownload3.helper.loge
import java.io.File


class DownloadService : Service() {
    private val missionBox = LocalMissionBox()
    private val binder = DownloadBinder()

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        logd("create")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logd("start")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        logd("bind")
        return binder
    }

    override fun onDestroy() {
        logd("destroy")
        missionBox.stopAll()
        super.onDestroy()
    }

    inner class DownloadBinder : Binder() {
        private val EMPTY_SUCCESS: (t: Any) -> Unit = {}

        fun create(statusCallback: StatusCallback, mission: Mission) {
            missionBox.create(mission).subscribe({
                statusCallback.apply(it)
            }, {
                loge("Create failed! ", it)
            })
        }

        fun start(mission: Mission) {
            missionBox.start(mission).subscribe(EMPTY_SUCCESS, {
                loge("Start failed! ", it)
            })
        }

        fun stop(mission: Mission) {
            missionBox.stop(mission).subscribe(EMPTY_SUCCESS, {
                loge("Stop failed! ", it)
            })
        }

        fun startAll() {
            missionBox.startAll().subscribe(EMPTY_SUCCESS, {
                loge("Start all failed! ", it)
            })
        }

        fun stopAll() {
            missionBox.stopAll().subscribe(EMPTY_SUCCESS, {
                loge("Stop all failed! ", it)
            })
        }

        fun getFile(fileCallback: FileCallback, mission: Mission) {
            missionBox.getFile(mission).subscribe({
                fileCallback.apply(it)
            }, {
                loge("Get file failed! ", it)
            })
        }

        fun extension(mission: Mission, type: Class<Extension>) {
            missionBox.extension(mission, type)
        }
    }


    interface StatusCallback {
        fun apply(status: Status)
    }

    interface FileCallback {
        fun apply(file: File)
    }
}