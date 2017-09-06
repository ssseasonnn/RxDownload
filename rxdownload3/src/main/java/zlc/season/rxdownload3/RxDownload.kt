package zlc.season.rxdownload3

import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.DownloadCore
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.status.Status


object RxDownload {
    private val downloadCore = DownloadCore()

    fun create(url: String): Flowable<Status> {
        return create(Mission(url))
    }

    fun create(mission: Mission): Flowable<Status> {
        return downloadCore.create(mission)
    }

    fun start(url: String): Maybe<Any> {
        return start(Mission(url))
    }

    fun start(mission: Mission): Maybe<Any> {
        return downloadCore.start(mission)
    }

    fun stop(url: String): Maybe<Any> {
        return stop(Mission(url))
    }

    fun stop(mission: Mission): Maybe<Any> {
        return downloadCore.stop(mission)
    }

    fun startALl(): Maybe<Any> {
        return downloadCore.startAll()
    }

    fun stopAll(): Maybe<Any> {
        return downloadCore.stopAll()
    }
}