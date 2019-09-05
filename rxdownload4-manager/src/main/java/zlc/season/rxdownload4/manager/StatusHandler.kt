package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.task.Task

class StatusHandler(
        private val task: Task,
        private val taskRecorder: TaskRecorder? = null,
        private var callback: (Status) -> Unit = {}
) {
    private val normal = Normal()
    private val started = Started()
    private val downloading = Downloading()
    private val paused = Paused()
    private val completed = Completed()
    private val failed = Failed()
    private val deleted = Deleted()

    var currentStatus: Status = normal

    private var currentProgress: Progress = Progress()

    fun setCallback(callback: (Status) -> Unit) {
        this.callback = callback
        callback(currentStatus)
    }

    fun onStarted() {
        currentStatus = started.updateProgress()
        callback(currentStatus)

        //try to insert
        taskRecorder?.insert(task)
    }

    fun onDownloading(next: Progress) {
        //set current progress
        currentProgress = next
        currentStatus = downloading.updateProgress()
        callback(currentStatus)

        taskRecorder?.update(task, currentStatus)
    }

    fun onCompleted() {
        currentStatus = completed.updateProgress()
        callback(currentStatus)

        taskRecorder?.update(task, currentStatus)
    }

    fun onFailed(t: Throwable) {
        currentStatus = failed.apply {
            progress = currentProgress
            throwable = t
        }
        callback(currentStatus)

        taskRecorder?.update(task, currentStatus)
    }

    fun onPaused() {
        currentStatus = paused.updateProgress()
        callback(currentStatus)

        taskRecorder?.update(task, currentStatus)
    }

    fun onDeleted() {
        //reset current progress
        currentProgress = Progress()
        currentStatus = deleted.updateProgress()
        callback(currentStatus)

        taskRecorder?.delete(task)
    }

    private fun Status.updateProgress(): Status {
        progress = currentProgress
        return this
    }
}