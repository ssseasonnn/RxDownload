package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.downloader.Dispatcher
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.validator.Validator
import zlc.season.rxdownload4.watcher.Watcher

object TaskManagerPool {
    private val map = mutableMapOf<Task, TaskManager>()

    private fun add(task: Task, taskManager: TaskManager) {
        map[task] = taskManager
    }

    private fun get(task: Task): TaskManager? {
        return map[task]
    }

    private fun remove(task: Task) {
        map.remove(task)
    }

    fun obtain(
            task: Task,
            header: Map<String, String>,
            maxConCurrency: Int,
            rangeSize: Long,
            dispatcher: Dispatcher,
            validator: Validator,
            storage: Storage,
            request: Request,
            watcher: Watcher,
            notificationCreator: NotificationCreator,
            recorder: TaskRecorder
    ): TaskManager {

        if (get(task) == null) {
            synchronized(this) {
                if (get(task) == null) {
                    val taskManager = task.createManager(
                            header = header,
                            maxConCurrency = maxConCurrency,
                            rangeSize = rangeSize,
                            dispatcher = dispatcher,
                            validator = validator,
                            storage = storage,
                            request = request,
                            watcher = watcher,
                            notificationCreator = notificationCreator,
                            recorder = recorder
                    )
                    add(task, taskManager)
                }
            }
        }

        return get(task)!!
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
            notificationCreator: NotificationCreator,
            recorder: TaskRecorder
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
        return TaskManager(
                task = this,
                storage = storage,
                taskRecorder = recorder,
                connectFlowable = download.publish(),
                notificationCreator = notificationCreator
        )
    }

}