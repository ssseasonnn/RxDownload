package zlc.season.rxdownload4.manager

import zlc.season.ironbranch.assertMainThreadWithResult
import zlc.season.ironbranch.ensureMainThread
import zlc.season.rxdownload4.DEFAULT_MAX_CONCURRENCY
import zlc.season.rxdownload4.DEFAULT_RANGE_SIZE
import zlc.season.rxdownload4.RANGE_CHECK_HEADER
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
        dispatcher: Dispatcher = DefaultDispatcher,
        validator: Validator = SimpleValidator,
        storage: Storage = SimpleStorage(),
        request: Request = RequestImpl,
        watcher: Watcher = WatcherImpl,
        notificationCreator: NotificationCreator = EmptyNotification,
        recorder: TaskRecorder = EmptyRecorder,
        taskLimitation: TaskLimitation = BasicTaskLimitation.of()
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
            notificationCreator = notificationCreator,
            recorder = recorder,
            taskLimitation = taskLimitation
    )
}

@JvmOverloads
fun Task.manager(
        header: Map<String, String> = RANGE_CHECK_HEADER,
        maxConCurrency: Int = DEFAULT_MAX_CONCURRENCY,
        rangeSize: Long = DEFAULT_RANGE_SIZE,
        dispatcher: Dispatcher = DefaultDispatcher,
        validator: Validator = SimpleValidator,
        storage: Storage = SimpleStorage(),
        request: Request = RequestImpl,
        watcher: Watcher = WatcherImpl,
        notificationCreator: NotificationCreator = EmptyNotification,
        recorder: TaskRecorder = EmptyRecorder,
        taskLimitation: TaskLimitation = BasicTaskLimitation.of()
): TaskManager {
    return TaskManagerPool.obtain(
            task = this,
            header = header,
            maxConCurrency = maxConCurrency,
            rangeSize = rangeSize,
            dispatcher = dispatcher,
            validator = validator,
            storage = storage,
            request = request,
            watcher = watcher,
            notificationCreator = notificationCreator,
            recorder = recorder,
            taskLimitation = taskLimitation
    )
}

fun TaskManager.subscribe(function: (Status) -> Unit): Any {
    return assertMainThreadWithResult {
        val tag = Any()
        addCallback(tag, true, function)
        return@assertMainThreadWithResult tag
    }
}

fun TaskManager.dispose(tag: Any) {
    ensureMainThread {
        removeCallback(tag)
    }
}

fun TaskManager.currentStatus(): Status {
    return assertMainThreadWithResult {
        return@assertMainThreadWithResult currentStatus()
    }
}

fun TaskManager.start() {
    ensureMainThread {
        taskLimitation.start(this)
    }
}

fun TaskManager.stop() {
    ensureMainThread {
        taskLimitation.stop(this)
    }
}

fun TaskManager.delete() {
    ensureMainThread {
        taskLimitation.delete(this)
    }
}

fun TaskManager.file(): File {
    return getFile()
}