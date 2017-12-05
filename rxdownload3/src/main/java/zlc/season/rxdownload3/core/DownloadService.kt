package zlc.season.rxdownload3.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import zlc.season.rxdownload3.extension.Extension
import zlc.season.rxdownload3.helper.logd
import java.io.File


class DownloadService : Service() {
    private val missionBox = LocalMissionBox()
    private val binder = DownloadBinder()


    override fun onCreate() {
        super.onCreate()
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
        fun isExists(mission: Mission, boolCallback: BoolCallback, errorCb: ErrorCallback) {
            missionBox.isExists(mission)
                    .subscribe(boolCallback::apply, errorCb::apply)
        }

        fun create(mission: Mission, statusCallback: StatusCallback) {
            missionBox.create(mission).subscribe(statusCallback::apply)
        }

        fun start(mission: Mission, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.start(mission).subscribe(successCb::apply, errorCb::apply)
        }

        fun stop(mission: Mission, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.stop(mission).subscribe(successCb::apply, errorCb::apply)
        }

        fun delete(mission: Mission, deleteFile: Boolean, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.delete(mission, deleteFile).subscribe(successCb::apply, errorCb::apply)
        }

        fun createAll(missions: List<Mission>, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.createAll(missions).subscribe(successCb::apply, errorCb::apply)
        }

        fun startAll(successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.startAll().subscribe(successCb::apply, errorCb::apply)
        }

        fun stopAll(successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.stopAll().subscribe(successCb::apply, errorCb::apply)
        }

        fun deleteAll(deleteFile: Boolean, successCallback: SuccessCallback, errorCallback: ErrorCallback) {
            missionBox.deleteAll(deleteFile).subscribe(successCallback::apply, errorCallback::apply)
        }

        fun file(mission: Mission, fileCallback: FileCallback, errorCb: ErrorCallback) {
            missionBox.file(mission).subscribe(fileCallback::apply, errorCb::apply)
        }

        fun extension(mission: Mission, type: Class<out Extension>,
                      successCallback: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.extension(mission, type)
                    .subscribe(successCallback::apply, errorCb::apply)
        }

        fun clear(mission: Mission, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.clear(mission)
                    .subscribe(successCb::apply, errorCb::apply)
        }

        fun clearAll(successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.clearAll()
                    .subscribe(successCb::apply, errorCb::apply)
        }

        fun update(newMission: Mission, successCb: SuccessCallback, errorCb: ErrorCallback) {
            missionBox.update(newMission)
                    .subscribe(successCb::apply, errorCb::apply)
        }
    }

    interface BoolCallback {
        fun apply(value: Boolean)
    }

    interface StatusCallback {
        fun apply(status: Status)
    }

    interface FileCallback {
        fun apply(file: File)
    }

    interface SuccessCallback {
        fun apply(any: Any)
    }

    interface ErrorCallback {
        fun apply(throwable: Throwable)
    }
}