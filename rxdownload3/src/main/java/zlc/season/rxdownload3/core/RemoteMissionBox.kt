package zlc.season.rxdownload3.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.reactivex.Flowable
import io.reactivex.Maybe


class RemoteMissionBox : MissionBox {

    lateinit var missionBox: MissionBox
    lateinit var context: Context

    init {

    }


    private fun startBindServiceAndDo(callback: (MissionBox) -> Unit) {
        val intent = Intent(context, DownloadService::class.java)
        context.startService(intent)
        context.bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val downloadBinder = binder as DownloadService.DownloadBinder
                val missionBox = downloadBinder.missionBox
                context.unbindService(this)
                callback(missionBox)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                //注意!!这个方法只会在系统杀掉Service时才会调用!!
            }
        }, Context.BIND_AUTO_CREATE)
    }

    override fun create(mission: Mission): Flowable<Status> {
        var flowable: Flowable<Status> = Flowable.empty()
        startBindServiceAndDo {
            flowable = it.create(mission)
        }
        return flowable
    }

    override fun start(mission: Mission): Maybe<Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop(mission: Mission): Maybe<Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startAll(): Maybe<Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stopAll(): Maybe<Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}