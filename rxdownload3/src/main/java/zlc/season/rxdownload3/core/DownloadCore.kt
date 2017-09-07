package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.plugins.RxJavaPlugins.setErrorHandler
import zlc.season.rxdownload3.helper.Logger.Companion.loge
import java.io.InterruptedIOException
import java.net.SocketException


class DownloadCore {
    init {
        initRxJavaPlugin()
    }

    private fun initRxJavaPlugin() {
        setErrorHandler {
            when (it) {
                is InterruptedException -> loge("InterruptedException", it)
                is InterruptedIOException -> loge("InterruptedIOException", it)
                is SocketException -> loge("SocketException", it)
            }
        }
    }

    fun create(mission: Mission): Flowable<Status> {
        return MissionBox.create(mission)
    }

    fun startAll(): Maybe<Any> {
        return MissionBox.startAll()
    }

    fun stopAll(): Maybe<Any> {
        return MissionBox.stopAll()
    }

    fun start(mission: Mission): Maybe<Any> {
        return MissionBox.start(mission)
    }

    fun stop(mission: Mission): Maybe<Any> {
        return MissionBox.stop(mission)
    }
}