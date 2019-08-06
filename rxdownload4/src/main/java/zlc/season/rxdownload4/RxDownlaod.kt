package zlc.season.rxdownload4

import io.reactivex.Flowable
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.task.Task


fun String.download(): Flowable<Status> {
    return Task(this).download()
}

fun Task.download(): Flowable<Status> {
    return Request().get(url, header)
            .flatMap {
                mapper.map(it).download(this, it)
            }
}