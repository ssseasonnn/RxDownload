package zlc.season.rxdownload3.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import zlc.season.rxdownload3.IDownloadCallback
import zlc.season.rxdownload3.IDownloadService


class DownloadService : Service() {
    private val missionBox = LocalMissionBox()
    private val binder = DownloadBinder()
    private val callbacks = RemoteCallbackList<IDownloadCallback>()

    override fun onCreate() {
        super.onCreate()
        DownloadConfig.init(this)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        missionBox.stopAll()
        callbacks.kill()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    inner class DownloadBinder : IDownloadService.Stub() {
        override fun start(mission: Mission?) {
            if (mission == null) return

            missionBox.start(mission).subscribe()
        }

        override fun create(callback: IDownloadCallback?, mission: Mission?) {
            if (callback == null || mission == null) return

            callbacks.register(callback)

            missionBox.create(mission).subscribe({
                callback.onUpdate(it)
            })
        }
    }

}