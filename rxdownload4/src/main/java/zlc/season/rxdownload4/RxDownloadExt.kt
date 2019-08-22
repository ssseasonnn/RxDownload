package zlc.season.rxdownload4

import io.reactivex.Flowable
import zlc.season.rxdownload4.downloader.DefaultDispatcher
import zlc.season.rxdownload4.downloader.Dispatcher
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.request.RequestImpl
import zlc.season.rxdownload4.storage.SimpleStorage
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.SharedTask
import zlc.season.rxdownload4.task.SharedTaskPool
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.safeDispose
import zlc.season.rxdownload4.validator.SimpleValidator
import zlc.season.rxdownload4.validator.Validator


/**
 * Returns a SharedTask represent the current task.
 *
 * A SharedTask means it can be subscribed in multiple places.
 */
fun Task.share(
        header: Map<String, String> = RANGE_CHECK_HEADER,
        maxConCurrency: Int = DEFAULT_MAX_CONCURRENCY,
        rangeSize: Long = DEFAULT_RANGE_SIZE,
        dispatcher: Dispatcher = DefaultDispatcher(),
        validator: Validator = SimpleValidator(),
        storage: Storage = SimpleStorage(),
        request: Request = RequestImpl()
): SharedTask {
    var sharedTask = SharedTaskPool.get(this)
    if (sharedTask == null) {
        sharedTask = createSharedTask(
                header = header,
                maxConCurrency = maxConCurrency,
                rangeSize = rangeSize,
                dispatcher = dispatcher,
                validator = validator,
                storage = storage,
                request = request
        )
        SharedTaskPool.add(this, sharedTask)
    }
    return sharedTask
}


fun SharedTask.get(): Flowable<Progress> {
    return connectableFlowable
}


/**
 * Start share download.
 */
fun SharedTask.start() {
    if (!isStarted()) {
        disposable = connectableFlowable.connect()
    }
}

/**
 * Stop share download
 */
fun SharedTask.stop() {
    disposable.safeDispose()
}

fun SharedTask.delete() {
    stop()
    SharedTaskPool.remove(task)
}

fun SharedTask.isStarted(): Boolean {
    return disposable != null && !disposable!!.isDisposed
}

fun SharedTask.isStoped(): Boolean {
    return disposable != null && disposable!!.isDisposed
}

private fun Task.createSharedTask(
        header: Map<String, String>,
        maxConCurrency: Int,
        rangeSize: Long,
        dispatcher: Dispatcher,
        validator: Validator,
        storage: Storage,
        request: Request
): SharedTask {

    val originFlowable = download(
            header = header,
            maxConCurrency = maxConCurrency,
            rangeSize = rangeSize,
            dispatcher = dispatcher,
            validator = validator,
            storage = storage,
            request = request
    )
    val connectableFlowable = originFlowable.replay(1)

    return SharedTask(this, connectableFlowable)
}