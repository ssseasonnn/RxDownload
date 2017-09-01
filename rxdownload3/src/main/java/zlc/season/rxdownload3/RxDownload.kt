package zlc.season.rxdownload3

import io.reactivex.Flowable
import zlc.season.rxdownload3.core.DownloadCore
import zlc.season.rxdownload3.core.DownloadStatus
import zlc.season.rxdownload3.core.Mission


object RxDownload {

    val downloadCore = DownloadCore()


    fun create(url: String): Flowable<DownloadStatus> {
        return create(Mission(url))
    }

    fun create(mission: Mission): Flowable<DownloadStatus> {
        return downloadCore.processMission(mission)
    }

    fun createAndStart(url: String): Flowable<DownloadStatus> {
        return createAndStart(Mission(url, autoStart = true))
    }

    fun createAndStart(mission: Mission): Flowable<DownloadStatus> {
        return downloadCore.processMission(mission)
    }

    fun download(url: String): Flowable<DownloadStatus> {
        return download(Mission(url))
    }

    fun download(mission: Mission): Flowable<DownloadStatus> {
        return downloadCore.processMission(mission)
    }


}