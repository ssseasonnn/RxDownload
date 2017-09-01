package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import zlc.season.rxdownload3.helper.Logger
import zlc.season.rxdownload3.helper.dispose
import java.io.InterruptedIOException
import java.net.SocketException


class DownloadCore {
    var disposable: Disposable? = null
    lateinit var observable: Observable<RealMission>


    init {
        initRxJavaPlugin()
        startMissionBox()
    }


    private fun initRxJavaPlugin() {
        RxJavaPlugins.setErrorHandler {
            when (it) {
                is InterruptedException -> Logger.loge("InterruptedException", it)
                is InterruptedIOException -> Logger.loge("InterruptedIOException", it)
                is SocketException -> Logger.loge("SocketException", it)
            }
        }
    }

    private fun startMissionBox() {
        observable = Observable.create<RealMission> {
            while (!it.isDisposed) {
                val mission = MissionBox.consume()
                it.onNext(mission)
            }
            it.onComplete()
        }.subscribeOn(Schedulers.io())

        disposable = observable.subscribe({ it.start() })
    }

    fun processMission(mission: Mission): Flowable<DownloadStatus> {
        return MissionBox.produce(mission)
    }

    fun start() {
        disposable = observable.subscribe({ it.start() })
    }

    fun stop() {
        dispose(disposable)
    }

    fun start(mission: Mission) {
        MissionBox.start(mission)
    }

    fun stop(mission: Mission) {
        MissionBox.stop(mission)
    }

}