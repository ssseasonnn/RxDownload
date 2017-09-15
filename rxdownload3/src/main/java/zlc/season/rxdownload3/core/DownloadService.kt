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
        override fun config(maxRange: Int, maxMission: Int, path: String?, enableDatabase: Boolean) {
            val configBuilder = ConfigBuilder.create(this@DownloadService)
                    .setMaxRange(maxRange)
                    .setMaxMission(maxMission)
                    .setDefaultPath(path!!)
                    .enableDataBase(enableDatabase)

            DownloadConfig.init(configBuilder)
        }

        override fun create(callback: IDownloadCallback?, mission: Mission?) {
            if (callback == null || mission == null) return

            callbacks.register(callback)

            missionBox.create(mission).subscribe({
                callback.onUpdate(it)
            })
        }

        override fun start(mission: Mission?) {
            if (mission == null) return

            missionBox.start(mission).subscribe()
        }

        override fun stop(mission: Mission?) {
            if (mission == null) return

            missionBox.stop(mission).subscribe()
        }

        override fun startAll() {
            missionBox.startAll().subscribe()
        }

        override fun stopAll() {
            missionBox.stopAll().subscribe()
        }
    }

}