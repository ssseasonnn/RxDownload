package zlc.season.rxdownload3

import io.reactivex.Observable
import zlc.season.rxdownload3.core.DownloadCore
import zlc.season.rxdownload3.core.DownloadMission
import zlc.season.rxdownload3.core.DownloadStatus
import zlc.season.rxdownload3.core.Mission


object RxDownload {

    val downloadCore = DownloadCore()


    fun download(url: String): Observable<DownloadStatus> {
        return download(DownloadMission(url))
    }

    fun download(mission: Mission): Observable<DownloadStatus> {
        return Observable.just(null)
    }


}