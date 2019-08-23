package zlc.season.rxdownload4.manager

import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import zlc.season.rxdownload4.*
import zlc.season.rxdownload4.downloader.DefaultDispatcher
import zlc.season.rxdownload4.downloader.Dispatcher
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.request.RequestImpl
import zlc.season.rxdownload4.storage.SimpleStorage
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.validator.SimpleValidator
import zlc.season.rxdownload4.validator.Validator

fun Task.manager(
        header: Map<String, String> = RANGE_CHECK_HEADER,
        maxConCurrency: Int = DEFAULT_MAX_CONCURRENCY,
        rangeSize: Long = DEFAULT_RANGE_SIZE,
        dispatcher: Dispatcher = DefaultDispatcher(),
        validator: Validator = SimpleValidator(),
        storage: Storage = SimpleStorage(),
        request: Request = RequestImpl()
): TaskManager {
    var taskManager = TaskManagerPool.get(this)
    if (taskManager == null) {
        taskManager = createManager(
                header = header,
                maxConCurrency = maxConCurrency,
                rangeSize = rangeSize,
                dispatcher = dispatcher,
                validator = validator,
                storage = storage,
                request = request
        )
        TaskManagerPool.add(this, taskManager)
    }
    return taskManager
}

private fun Task.createManager(
        header: Map<String, String>,
        maxConCurrency: Int,
        rangeSize: Long,
        dispatcher: Dispatcher,
        validator: Validator,
        storage: Storage,
        request: Request
): TaskManager {

    val flowable = download(
            header = header,
            maxConCurrency = maxConCurrency,
            rangeSize = rangeSize,
            dispatcher = dispatcher,
            validator = validator,
            storage = storage,
            request = request
    )
    val processor = BehaviorProcessor.create<Status>().toSerialized()
    return TaskManager(this, flowable, processor)
}


fun TaskManager.status(): Flowable<Status> {
    return innerGet()
}

fun TaskManager.currentStatus(): Status {
    return innerStatus()
}

fun TaskManager.currentProgress(): Progress {
    return progress()
}

fun TaskManager.start() {
    innerStart()
}

fun TaskManager.stop() {
    innerStop()
}

fun TaskManager.delete() {

}