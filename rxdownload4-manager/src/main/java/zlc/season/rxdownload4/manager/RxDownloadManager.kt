package zlc.season.rxdownload4.manager

import io.reactivex.Flowable
import zlc.season.ironbranch.ensureMainThread
import zlc.season.rxdownload4.DEFAULT_MAX_CONCURRENCY
import zlc.season.rxdownload4.DEFAULT_RANGE_SIZE
import zlc.season.rxdownload4.RANGE_CHECK_HEADER
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.downloader.DefaultDispatcher
import zlc.season.rxdownload4.downloader.Dispatcher
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.request.RequestImpl
import zlc.season.rxdownload4.storage.SimpleStorage
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.validator.SimpleValidator
import zlc.season.rxdownload4.validator.Validator
import zlc.season.rxdownload4.watcher.Watcher
import zlc.season.rxdownload4.watcher.WatcherImpl
import java.io.File

@JvmOverloads
fun String.manager(
        header: Map<String, String> = RANGE_CHECK_HEADER,
        maxConCurrency: Int = DEFAULT_MAX_CONCURRENCY,
        rangeSize: Long = DEFAULT_RANGE_SIZE,
        dispatcher: Dispatcher = DefaultDispatcher(),
        validator: Validator = SimpleValidator(),
        storage: Storage = SimpleStorage(),
        request: Request = RequestImpl(),
        watcher: Watcher = WatcherImpl(),
        notificationCreator: NotificationCreator = EmptyNotification()
): TaskManager {
    return Task(this).manager(
            header = header,
            maxConCurrency = maxConCurrency,
            rangeSize = rangeSize,
            dispatcher = dispatcher,
            validator = validator,
            storage = storage,
            request = request,
            watcher = watcher,
            notificationCreator = notificationCreator
    )
}

@JvmOverloads
@Synchronized
fun Task.manager(
        header: Map<String, String> = RANGE_CHECK_HEADER,
        maxConCurrency: Int = DEFAULT_MAX_CONCURRENCY,
        rangeSize: Long = DEFAULT_RANGE_SIZE,
        dispatcher: Dispatcher = DefaultDispatcher(),
        validator: Validator = SimpleValidator(),
        storage: Storage = SimpleStorage(),
        request: Request = RequestImpl(),
        watcher: Watcher = WatcherImpl(),
        notificationCreator: NotificationCreator = EmptyNotification()
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
                request = request,
                watcher = watcher,
                notificationCreator = notificationCreator
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
        request: Request,
        watcher: Watcher,
        notificationCreator: NotificationCreator
): TaskManager {

    val download = download(
            header = header,
            maxConCurrency = maxConCurrency,
            rangeSize = rangeSize,
            dispatcher = dispatcher,
            validator = validator,
            storage = storage,
            request = request,
            watcher = watcher
    )
    return TaskManager(this, storage, download.publish(), notificationCreator)
}


fun TaskManager.subscribe(function: (Status) -> Unit) {
    setCallback(function)
}

fun TaskManager.dispose() {
    setCallback()
}

fun TaskManager.currentStatus(): Status {
    return currentStatus()
}

fun TaskManager.start() {
    ensureMainThread {
        innerStart()
    }
}

fun TaskManager.stop() {
    ensureMainThread {
        innerStop()
    }
}

fun TaskManager.delete() {
    ensureMainThread {
        innerDelete()
    }
}

fun TaskManager.file(): File {
    return getFile()
}

fun TaskManager.getAll(): Flowable<List<Task>> {
    return taskDatabase.getAll()
}