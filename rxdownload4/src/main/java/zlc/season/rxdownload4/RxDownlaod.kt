package zlc.season.rxdownload4

import io.reactivex.Flowable


fun String.download(): Flowable<Status> {
    val api = api<DownloadApi>()
    val flowable = api.download(this)
            .flatMap {
                return@flatMap it.map().download(it)
            }

    return flowable
}