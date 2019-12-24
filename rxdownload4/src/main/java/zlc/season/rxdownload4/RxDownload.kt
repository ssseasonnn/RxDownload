package zlc.season.rxdownload4

import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import io.reactivex.Flowable
import zlc.season.rxdownload4.downloader.DefaultDispatcher
import zlc.season.rxdownload4.downloader.Dispatcher
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.request.RequestImpl
import zlc.season.rxdownload4.storage.SimpleStorage
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.task.TaskInfo
import zlc.season.rxdownload4.utils.clear
import zlc.season.rxdownload4.utils.log
import zlc.season.rxdownload4.validator.SimpleValidator
import zlc.season.rxdownload4.validator.Validator
import zlc.season.rxdownload4.watcher.Watcher
import zlc.season.rxdownload4.watcher.WatcherImpl
import java.io.File


val DEFAULT_SAVE_PATH: String = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path

val RANGE_CHECK_HEADER = mapOf("Range" to "bytes=0-")

const val DEFAULT_RANGE_SIZE = 5L * 1024 * 1024 //5M

const val DEFAULT_MAX_CONCURRENCY = 3


/**
 * Returns a Download Flowable represent the current url.
 */
@JvmOverloads
fun String.download(
        header: Map<String, String> = RANGE_CHECK_HEADER,
        maxConCurrency: Int = DEFAULT_MAX_CONCURRENCY,
        rangeSize: Long = DEFAULT_RANGE_SIZE,
        dispatcher: Dispatcher = DefaultDispatcher,
        validator: Validator = SimpleValidator,
        storage: Storage = SimpleStorage,
        request: Request = RequestImpl,
        watcher: Watcher = WatcherImpl
): Flowable<Progress> {
    require(rangeSize > 1024 * 1024) { "rangeSize must be greater than 1M" }
    require(maxConCurrency > 0) { "maxConCurrency must be greater than 0" }

    return Task(this).download(
            header = header,
            maxConCurrency = maxConCurrency,
            rangeSize = rangeSize,
            dispatcher = dispatcher,
            validator = validator,
            storage = storage,
            request = request,
            watcher = watcher
    )
}

@JvmOverloads
fun String.file(storage: Storage = SimpleStorage): File {
    return Task(this).file(storage)
}

@JvmOverloads
fun String.delete(storage: Storage = SimpleStorage) {
    Task(this).delete(storage)
}

/**
 * Returns a Download Flowable represent the current task.
 */
@JvmOverloads
fun Task.download(
        header: Map<String, String> = RANGE_CHECK_HEADER,
        maxConCurrency: Int = DEFAULT_MAX_CONCURRENCY,
        rangeSize: Long = DEFAULT_RANGE_SIZE,
        dispatcher: Dispatcher = DefaultDispatcher,
        validator: Validator = SimpleValidator,
        storage: Storage = SimpleStorage,
        request: Request = RequestImpl,
        watcher: Watcher = WatcherImpl
): Flowable<Progress> {
    require(rangeSize > 1024 * 1024) { "rangeSize must be greater than 1M" }
    require(maxConCurrency > 0) { "maxConCurrency must be greater than 0" }

    val taskInfo = TaskInfo(
            task = this,
            header = header,
            maxConCurrency = maxConCurrency,
            rangeSize = rangeSize,
            dispatcher = dispatcher,
            validator = validator,
            storage = storage,
            request = request,
            watcher = watcher
    )

    return taskInfo.start()
}

@JvmOverloads
fun Task.file(storage: Storage = SimpleStorage): File {
    storage.load(this)
    if (isEmpty()) {
        "Task file not found".log()
    }
    return File(savePath, saveName)
}

@JvmOverloads
fun Task.delete(storage: Storage = SimpleStorage) {
    val file = file(storage)
    file.clear()
    storage.delete(this)
}
