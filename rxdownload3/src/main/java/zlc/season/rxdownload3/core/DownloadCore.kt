package zlc.season.rxdownload3.core

import io.reactivex.Observable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import zlc.season.rxdownload3.helper.Logger
import java.io.InterruptedIOException
import java.net.SocketException


class DownloadCore {

    init {
        initRxJavaPlugin()
        startMissionBox()
    }


    private fun initRxJavaPlugin() {
        RxJavaPlugins.setErrorHandler { t: Throwable ->
            if (t is InterruptedException) {
                Logger.loge("InterruptedException", t)
            } else if (t is InterruptedIOException) {
                Logger.loge("InterruptedIOException", t)
            } else if (t is SocketException) {
                Logger.loge("SocketException", t)
            }
        }
    }


    private fun startMissionBox() {
        Observable.create<RealMission> { e ->
            while (!e.isDisposed) {
                val mission = MissionBox.consume()
                e.onNext(mission)
            }
            e.onComplete()
        }.subscribeOn(Schedulers.io()).subscribe({ t: RealMission ->
            t.start()
        }, { t ->
            Logger.loge("Mission Failed", t)
        }, {

        })
    }

    fun processMission(mission: Mission): Observable<DownloadStatus> {
        return MissionBox.produce(mission)
    }


}