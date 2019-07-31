package zlc.season.rxdownload4

import io.reactivex.Flowable
import zlc.season.rxdownload4.utils.map
import java.io.File


fun String.download(): Flowable<Status> {
    val request = request<Request>()
    val flowable = request.get(this)
            .flatMap {
                return@flatMap it.map().download(it)
            }

    return flowable
}

fun String.rangeDownload(): Flowable<Status> {
    val download = RangeDownloader()
    val request = request<Request>()
    val flowable = request.checkWithHead(this)
            .flatMap {
                if (it.isSuccessful) {
                    request.download(this)
                } else {
                    throw RuntimeException(it.message())
                }
            }
            .flatMap {
                return@flatMap it.map().download(it)
            }

    return flowable
}

fun Task.rangeDownload(): Flowable<Status> {
    val file = File(savePath, saveName)
    val download = RangeDownloader()


    return Flowable.just(Status())
}