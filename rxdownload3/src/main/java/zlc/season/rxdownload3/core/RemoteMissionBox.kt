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
import zlc.season.rxdownload3.core.DownloadConfig.ANY


class RemoteMissionBox : MissionBox {
    var context: Context = DownloadConfig.context

    init {
        Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.config(
                        DownloadConfig.maxRange,
                        DownloadConfig.maxMission,
                        DownloadConfig.defaultSavePath,
                        DownloadConfig.enableDataBase
                )
                emitter.onSuccess(ANY)
            }
        }.subscribeOn(newThread()).subscribe()
    }

    override fun create(mission: Mission): Flowable<Status> {
        return Flowable.create<Status>({ emitter ->
            startBindServiceAndDo {
                val callback = object : IDownloadCallback.Stub() {
                    override fun onUpdate(status: Status?) {
                        status as Status
                        emitter.onNext(status)
                    }
                }
                it.create(callback, mission)
            }
        }, LATEST).subscribeOn(newThread())
    }

    override fun start(mission: Mission): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.start(mission)
                emitter.onSuccess(ANY)
            }
        }.subscribeOn(newThread())
    }

    override fun stop(mission: Mission): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.stop(mission)
                emitter.onSuccess(ANY)
            }
        }.subscribeOn(newThread())
    }

    override fun startAll(): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.startAll()
                emitter.onSuccess(ANY)
            }
        }.subscribeOn(newThread())
    }

    override fun stopAll(): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.stopAll()
                emitter.onSuccess(ANY)
            }
        }.subscribeOn(newThread())
    }


    private fun startBindServiceAndDo(callback: (IDownloadService) -> Unit) {
        val intent = Intent(context, DownloadService::class.java)
        context.startService(intent)
        context.bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val service = IDownloadService.Stub.asInterface(binder)
                callback(service!!)
                context.unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName) {
            }
        }, BIND_AUTO_CREATE)
    }
}
