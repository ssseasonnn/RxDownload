package zlc.season.rxdownload3.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.reactivex.Flowable
import io.reactivex.Maybe


class DownloadService : Service(), MissionBox {
    val missionBox = LocalMissionBox()
    val binder = DownloadBinder()

    override fun create(mission: Mission): Flowable<Status> {
        return missionBox.create(mission)
    }

    override fun start(mission: Mission): Maybe<Any> {
        return missionBox.start(mission)
    }

    override fun stop(mission: Mission): Maybe<Any> {
        return missionBox.stop(mission)
    }

    override fun startAll(): Maybe<Any> {
        return missionBox.startAll()
    }

    override fun stopAll(): Maybe<Any> {
        return missionBox.stopAll()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class DownloadBinder : Binder() {
        val missionBox: MissionBox
            get() = this@DownloadService
    }
}