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
import zlc.season.rxdownload3.extension.Extension
import java.io.File


class RemoteMissionBox : MissionBox {
    var context: Context = DownloadConfig.context

    override fun create(mission: Mission): Flowable<Status> {
        return Flowable.create<Status>({ emitter ->
            startBindServiceAndDo {
                it.create(object : DownloadService.StatusCallback {
                    override fun apply(status: Status) {
                        emitter.onNext(status)
                    }
                }, mission)
            }
        }, LATEST).subscribeOn(newThread())
    }

    override fun start(mission: Mission): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.start(mission, object : DownloadService.SuccessCallback {
                    override fun apply(any: Any) {
                        emitter.onSuccess(any)
                    }
                }, object : DownloadService.ErrorCallback {
                    override fun apply(throwable: Throwable) {
                        emitter.onError(throwable)
                    }
                })
            }
        }.subscribeOn(newThread())
    }

    override fun stop(mission: Mission): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.stop(mission, object : DownloadService.SuccessCallback {
                    override fun apply(any: Any) {
                        emitter.onSuccess(any)
                    }
                }, object : DownloadService.ErrorCallback {
                    override fun apply(throwable: Throwable) {
                        emitter.onError(throwable)
                    }
                })
            }
        }.subscribeOn(newThread())
    }

    override fun delete(mission: Mission): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.delete(mission, object : DownloadService.SuccessCallback {
                    override fun apply(any: Any) {
                        emitter.onSuccess(any)
                    }
                }, object : DownloadService.ErrorCallback {
                    override fun apply(throwable: Throwable) {
                        emitter.onError(throwable)
                    }
                })
            }
        }.subscribeOn(newThread())
    }

    override fun startAll(): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.startAll(object : DownloadService.SuccessCallback {
                    override fun apply(any: Any) {
                        emitter.onSuccess(any)
                    }
                }, object : DownloadService.ErrorCallback {
                    override fun apply(throwable: Throwable) {
                        emitter.onError(throwable)
                    }
                })
            }
        }.subscribeOn(newThread())
    }

    override fun stopAll(): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.stopAll(object : DownloadService.SuccessCallback {
                    override fun apply(any: Any) {
                        emitter.onSuccess(any)
                    }
                }, object : DownloadService.ErrorCallback {
                    override fun apply(throwable: Throwable) {
                        emitter.onError(throwable)
                    }
                })
            }
        }.subscribeOn(newThread())
    }

    override fun file(mission: Mission): Maybe<File> {
        return Maybe.create<File> { emitter ->
            startBindServiceAndDo {
                it.file(mission, object : DownloadService.FileCallback {
                    override fun apply(file: File) {
                        emitter.onSuccess(file)
                    }
                }, object : DownloadService.ErrorCallback {
                    override fun apply(throwable: Throwable) {
                        emitter.onError(throwable)
                    }
                })
            }
        }.subscribeOn(newThread())
    }

    override fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any> {
        return Maybe.create<Any> { emitter ->
            startBindServiceAndDo {
                it.extension(mission, type,
                        object : DownloadService.ExtensionCallback {
                            override fun apply(any: Any) {
                                emitter.onSuccess(any)
                            }
                        },
                        object : DownloadService.ErrorCallback {
                            override fun apply(throwable: Throwable) {
                                emitter.onError(throwable)
                            }
                        })
            }
        }
    }

    var downloadBinder: DownloadService.DownloadBinder? = null

    private fun startBindServiceAndDo(callback: (DownloadService.DownloadBinder) -> Unit) {
        if (downloadBinder != null) {
            callback(downloadBinder!!)
            return
        }

        val intent = Intent(context, DownloadService::class.java)
        context.startService(intent)
        context.bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                downloadBinder = binder as DownloadService.DownloadBinder
                callback(downloadBinder!!)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                downloadBinder = null
            }
        }, BIND_AUTO_CREATE)
    }
}
