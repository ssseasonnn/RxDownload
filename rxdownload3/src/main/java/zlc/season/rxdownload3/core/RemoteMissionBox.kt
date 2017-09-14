package zlc.season.rxdownload3.core

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.reactivex.BackpressureStrategy.LATEST
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers.newThread
import zlc.season.rxdownload3.IDownloadCallback
import zlc.season.rxdownload3.IDownloadService


class RemoteMissionBox : MissionBox {

    var context: Context = DownloadConfig.applicationContext

    override fun create(mission: Mission): Flowable<Status> {
        return Flowable.create<Status>({ emitter ->
            startBindServiceAndDo {
                val callback = object : IDownloadCallback.Stub() {
                    override fun onUpdate(status: Status?) {
                        if (status == null) return
                        emitter.onNext(status)
                    }
                }
                it.registerDownloadCallback(callback, mission)
            }
        }, LATEST).subscribeOn(newThread())
    }

    override fun start(mission: Mission): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.start(mission)
                emitter.onSuccess(Any())
            }
        }.subscribeOn(newThread())
    }

    override fun stop(mission: Mission): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                //                it.stop(emitter, mission)
            }
        }.subscribeOn(newThread())
    }

    override fun startAll(): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                //                it.startAll(emitter)
            }
        }.subscribeOn(newThread())
    }

    override fun stopAll(): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                //                it.stopAll(emitter)
            }
        }.subscribeOn(newThread())
    }

    private fun startBindServiceAndDo(callback: (IDownloadService) -> Unit) {
        val intent = Intent(context, DownloadService::class.java)
        context.startService(intent)
        context.bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
//                val downloadBinder = binder as DownloadService.DownloadBinder
                val downloadBinder = IDownloadService.Stub.asInterface(binder)
                callback(downloadBinder)
                context.unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName) {
            }
        }, BIND_AUTO_CREATE)
    }
}
