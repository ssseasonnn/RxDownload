package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.log

class StatusHandler(
        private val task: Task,
        private val taskRecorder: TaskRecorder? = null,
        private val logTag: String = "",
        callback: (Status) -> Unit = {}
) {
    private val normal = Normal()
    private val started = Started()
    private val downloading = Downloading()
    private val paused = Paused()
    private val completed = Completed()
    private val failed = Failed()
    private val deleted = Deleted()

    var currentStatus: Status = normal

    private val callbackMap = mutableMapOf<Any, (Status) -> Unit>()

    private var currentProgress: Progress = Progress()

    init {
        callbackMap[Any()] = callback
    }

    fun addCallback(tag: Any, callback: (Status) -> Unit) {
        callbackMap[tag] = callback

        //emit last status when not normal
        if (currentStatus != normal) {
            callback(currentStatus)
        }
    }

    fun removeCallback(tag: Any) {
        callbackMap.remove(tag)
    }

    fun onStarted() {
        currentStatus = started.updateProgress()
        dispatchCallback()

        //try to insert
        taskRecorder?.insert(task)

        "$logTag [${task.taskName}] started".log()
    }

    fun onDownloading(next: Progress) {
        //set current progress
        currentProgress = next
        currentStatus = downloading.updateProgress()
        dispatchCallback()

        taskRecorder?.update(task, currentStatus)

        "$logTag [${task.taskName}] downloading".log()
    }

    fun onCompleted() {
        currentStatus = completed.updateProgress()
        dispatchCallback()

        taskRecorder?.update(task, currentStatus)

        "$logTag [${task.taskName}] completed".log()
    }

    fun onFailed(t: Throwable) {
        currentStatus = failed.apply {
            progress = currentProgress
            throwable = t
        }
        dispatchCallback()

        taskRecorder?.update(task, currentStatus)

        "$logTag [${task.taskName}] failed".log()
    }

    fun onPaused() {
        currentStatus = paused.updateProgress()
        dispatchCallback()

        taskRecorder?.update(task, currentStatus)

        "$logTag [${task.taskName}] paused".log()
    }

    fun onDeleted() {
        //reset current progress
        currentProgress = Progress()
        currentStatus = deleted.updateProgress()
        dispatchCallback()

        //delete
        taskRecorder?.delete(task)

        "$logTag [${task.taskName}] deleted".log()
    }

    private fun dispatchCallback() {
        callbackMap.values.forEach {
            it(currentStatus)
        }
    }

    private fun Status.updateProgress(): Status {
        progress = currentProgress
        return this
    }
}