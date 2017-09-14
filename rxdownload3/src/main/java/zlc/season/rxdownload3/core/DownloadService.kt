package zlc.season.rxdownload3.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import zlc.season.rxdownload3.IDownloadCallback
import zlc.season.rxdownload3.IDownloadService


class DownloadService : Service() {
    private val missionBox = LocalMissionBox()
    private val binder = BBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onStart")
        DownloadConfig.init(this)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        println("onTaskRemoved")
    }


    val callbacks = RemoteCallbackList<IDownloadCallback>()

    inner class BBinder : IDownloadService.Stub() {
        override fun start(mission: Mission?) {
            if (mission == null) return

            missionBox.start(mission).subscribe()
        }

        override fun registerDownloadCallback(callback: IDownloadCallback?, mission: Mission?) {
            if (callback == null || mission == null) return

            callbacks.register(callback)

            missionBox.create(mission).subscribe({
                println(it.javaClass.canonicalName)
                callback.onUpdate(it)
            })
        }

        override fun unregisterDownloadCallback(callback: IDownloadCallback?) {
            callbacks.unregister(callback)
        }

    }

}