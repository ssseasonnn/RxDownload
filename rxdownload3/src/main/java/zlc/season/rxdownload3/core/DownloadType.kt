package zlc.season.rxdownload3.core

import io.reactivex.Flowable


interface DownloadType {
    fun download(): Flowable<DownloadStatus>
}