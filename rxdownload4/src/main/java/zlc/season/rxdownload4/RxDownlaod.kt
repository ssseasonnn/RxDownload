package zlc.season.rxdownload4

import io.reactivex.Flowable
import zlc.season.rxdownload4.utils.map


fun String.download(): Flowable<Status> {
    val request = request<Request>()
    val flowable = request.get(this)
            .flatMap {
                return@flatMap it.map().download(it)
            }

    return flowable
}