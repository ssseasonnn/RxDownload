package zlc.season.rxdownload3.core

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import zlc.season.rxdownload3.extension.Extension
import zlc.season.rxdownload3.helper.logd
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
        fun create(statusCallback: StatusCallback, mission: Mission) {
            missionBox.create(mission).subscribe(statusCallback::apply)
        }

        fun start(mission: Mission, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.start(mission).subscribe(successCb::apply, errorCb::apply)
        }

        fun stop(mission: Mission, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.stop(mission).subscribe(successCb::apply, errorCb::apply)
        }

        fun delete(mission: Mission, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.delete(mission).subscribe(successCb::apply, errorCb::apply)
        }

        fun startAll(successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.startAll().subscribe(successCb::apply, errorCb::apply)
        }

        fun stopAll(successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.stopAll().subscribe(successCb::apply, errorCb::apply)
        }

        fun file(mission: Mission, fileCallback: FileCallback, errorCb: ErrorCallback) {
            missionBox.file(mission).subscribe(fileCallback::apply, errorCb::apply)
        }

        fun extension(mission: Mission, type: Class<out Extension>,
                      extensionCallback: ExtensionCallback, errorCb: ErrorCallback) {
            missionBox.extension(mission, type)
                    .subscribe(extensionCallback::apply, errorCb::apply)
        }
    }


    interface StatusCallback {
        fun apply(status: Status)
    }

    interface FileCallback {
        fun apply(file: File)
    }

    interface ExtensionCallback {
        fun apply(any: Any)
    }

    interface SuccessCallback {
        fun apply(any: Any)
    }

    interface ErrorCallback {
        fun apply(throwable: Throwable)
    }
}