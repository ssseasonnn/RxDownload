package zlc.season.rxdownload4.task

import io.reactivex.Flowable
import zlc.season.rxdownload4.DEFAULT_SAVE_PATH
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.downloader.Dispatcher
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.utils.fileName
import zlc.season.rxdownload4.validator.Validator
import zlc.season.rxdownload4.watcher.Watcher

class TaskInfo(
        val task: Task,
        val header: Map<String, String>,
        val maxConCurrency: Int,
        val rangeSize: Long,
        val dispatcher: Dispatcher,
        val validator: Validator,
        val storage: Storage,
        val request: Request,
        val watcher: Watcher
) {
    fun start(): Flowable<Progress> {
        //Before start download, we should load task first.
        storage.load(task)

        //Identify if the task is being watched.
        var watchFlag = false

        return request.get(task.url, header)
                .doOnNext {
                    check(it.isSuccessful) { "Request failed!" }

                    if (task.saveName.isEmpty()) {
                        task.saveName = it.fileName()
                    }
                    if (task.savePath.isEmpty()) {
                        task.savePath = DEFAULT_SAVE_PATH
                    }

                    try {
                        //Watch task, should be done when the task
                        //has save path and save name.
                        watcher.watch(task)
                        watchFlag = true
                    } catch (e: Throwable) {
                        throw e
                    }

                    //save task info
                    storage.save(task)
                }
                .flatMap {
                    dispatcher.dispatch(it).download(this, it)
                }
                .doFinally {
                    //unwatch task
                    if (watchFlag) {
                        watcher.unwatch(task)
                    }
                }
    }
}