package zlc.season.rxdownload4

import io.reactivex.Flowable
import zlc.season.rxdownload4.request.Request


fun String.download(): Flowable<Status> {
    return Request().get(this)
            .flatMap {
                DEFAULT_MAPPER.map(it).download(it)
            }
}