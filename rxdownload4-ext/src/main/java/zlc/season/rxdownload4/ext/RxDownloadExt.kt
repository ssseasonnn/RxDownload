package zlc.season.rxdownload4.ext

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.rxdownload4.*
import zlc.season.rxdownload4.downloader.DefaultDispatcher
import zlc.season.rxdownload4.downloader.Dispatcher
import zlc.season.rxdownload4.management.SharedTask
import zlc.season.rxdownload4.management.SharedTaskPool
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.request.RequestImpl
import zlc.season.rxdownload4.storage.SimpleStorage
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.validator.SimpleValidator
import zlc.season.rxdownload4.validator.Validator


/**
 * Returns a Shared Download Flowable represent the current task.
 *
 * A Shared Download Flowable means it can be received by multiple down streams.
 */
fun Task.shareDownload(
        header: Map<String, String> = RANGE_CHECK_HEADER,
        maxConCurrency: Int = DEFAULT_MAX_CONCURRENCY,
        rangeSize: Long = DEFAULT_RANGE_SIZE,
        dispatcher: Dispatcher = DefaultDispatcher(),
        validator: Validator = SimpleValidator(),
        storage: Storage = SimpleStorage(),
        request: Request = RequestImpl(),
        start: Boolean = false
): Flowable<Progress> {

    val sharedTask = sharedTask {
        createNewSharedTask(
                header = header,
                maxConCurrency = maxConCurrency,
                rangeSize = rangeSize,
                dispatcher = dispatcher,
                validator = validator,
                storage = storage,
                request = request,
                start = start
        )
    }
    return sharedTask.flowable
}

private fun Task.sharedTask(taskBuilder: () -> SharedTask): SharedTask {
    var sharedTask = SharedTaskPool.get(this)
    if (sharedTask == null) {
        val newSharedTask = taskBuilder()
        SharedTaskPool.add(this, newSharedTask)
        sharedTask = newSharedTask
    } else {
        if (sharedTask.disposable.isDisposed) {
            val newSharedTask = taskBuilder()
            SharedTaskPool.add(this, newSharedTask)
            sharedTask = newSharedTask
        }
    }
    return sharedTask
}

/**
 * Returns a Disposable represent the Shared Download Flowable.
 *
 * When you call [Disposable.dispose] method, all downstream of the Shared Flowable will stop.
 */
fun Task.shareDisposable(): Disposable {
    val sharedTask = SharedTaskPool.get(this)
            ?: throw IllegalStateException("Shared Task does not exists! Call shareDownload first!")
    return sharedTask.disposable
}

private fun Task.createNewSharedTask(
        header: Map<String, String>,
        maxConCurrency: Int,
        rangeSize: Long,
        dispatcher: Dispatcher,
        validator: Validator,
        storage: Storage,
        request: Request,
        start: Boolean
): SharedTask {

    val flowable = download(
            header = header,
            maxConCurrency = maxConCurrency,
            rangeSize = rangeSize,
            dispatcher = dispatcher,
            validator = validator,
            storage = storage,
            request = request
    ).replay(1).also {
        if (start) {
            it.subscribeBy()
        }
    }
    val disposable = flowable.connect()
    return SharedTask(flowable, disposable)
}