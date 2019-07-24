package zlc.season.rxdownload4

import io.reactivex.Flowable


fun String.download(): Flowable<Status> {
    return Requests.get().download(this, emptyMap())
            .flatMap {

            }
}