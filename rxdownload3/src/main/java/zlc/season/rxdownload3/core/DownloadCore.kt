package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.plugins.RxJavaPlugins.setErrorHandler
import zlc.season.rxdownload3.extension.Extension
import zlc.season.rxdownload3.helper.loge
import java.io.File
import java.io.InterruptedIOException
import java.net.SocketException


class DownloadCore {
    private val missionBox = DownloadConfig.missionBox

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
        return missionBox.create(mission)
    }

    fun startAll(): Maybe<Any> {
        return missionBox.startAll()
    }

    fun stopAll(): Maybe<Any> {
        return missionBox.stopAll()
    }

    fun start(mission: Mission): Maybe<Any> {
        return missionBox.start(mission)
    }

    fun stop(mission: Mission): Maybe<Any> {
        return missionBox.stop(mission)
    }

    fun delete(mission: Mission): Maybe<Any> {
        return missionBox.delete(mission)
    }

    fun file(mission: Mission): Maybe<File> {
        return missionBox.file(mission)
    }

    fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any> {
        return missionBox.extension(mission, type)
    }

    fun getAllMission(): Maybe<List<Mission>> {
        val enableDb = DownloadConfig.enableDb
        return if (enableDb) {
            val dbActor = DownloadConfig.dbActor
            dbActor.getAllMission()
        } else {
            Maybe.just(emptyList())
        }
    }
}