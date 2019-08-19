package zlc.season.rxdownload4.task

import io.reactivex.Flowable
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.downloader.Dispatcher
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.validator.Validator

class TaskInfo(
        val task: Task,
        val header: Map<String, String>,
        val maxConCurrency: Int,
        val rangeSize: Long,
        val dispatcher: Dispatcher,
        val validator: Validator,
        val storage: Storage,
        val request: Request
) {
    fun download(): Flowable<Progress> {
        return request.get(task.url, header)
                .flatMap {
                    dispatcher.dispatch(it).download(this, it)
                }
    }
}