package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
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
        observable = Observable.create<RealMission> { loop(it) }
                .subscribeOn(Schedulers.io())
                .doOnComplete { Logger.logd("DownloadCore onComplete!") }
                .doOnDispose { Logger.logd("DownloadCore onDispose!") }
                .doOnError { Logger.loge("DownloadCore onError!", it) }
                .doOnNext { Logger.logd("DownloadCore onNext! Mission url: ${it.mission.url}") }

        disposable = observable.subscribe({ it.create() })
    }

    private fun loop(it: ObservableEmitter<RealMission>) {
        while (!it.isDisposed) {
            val mission = MissionBox.consume()
            it.onNext(mission)
        }
        it.onComplete()
    }

    fun processMission(mission: Mission): Flowable<DownloadStatus> {
        return MissionBox.produce(mission)
    }

    fun start() {
        disposable = observable.subscribe({ it.create() })
    }

    fun stop() {
        dispose(disposable)
    }

    fun start(mission: Mission): Maybe<Any> {
        return MissionBox.start(mission)
    }

    fun stop(mission: Mission) {
        MissionBox.stop(mission)
    }

}