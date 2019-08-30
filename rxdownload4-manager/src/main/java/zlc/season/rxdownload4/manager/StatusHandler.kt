package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.Progress

class StatusHandler(var callback: (Status) -> Unit = {}) {
    private val normal = Normal()
    private val started = Started()
    private val downloading = Downloading()
    private val paused = Paused()
    private val completed = Completed()
    private val failed = Failed()
    private val deleted = Deleted()

    var currentStatus: Status = normal
    private var currentProgress: Progress = Progress()

    fun onNormal() {
        //reset current progress
        currentProgress = Progress()

        currentStatus = normal.updateProgress()
        callback(currentStatus)
    }

    fun onStarted() {
        currentStatus = started.updateProgress()
        callback(currentStatus)
    }

    fun onDownloading(next: Progress) {
        //set current progress
        currentProgress = next

        currentStatus = downloading.updateProgress()
        callback(currentStatus)
    }

    fun onCompleted() {
        currentStatus = completed.updateProgress()
        callback(currentStatus)
    }

    fun onFailed(t: Throwable) {
        currentStatus = failed.apply {
            progress = currentProgress
            throwable = t
        }
        callback(currentStatus)
    }

    fun onPaused() {
        currentStatus = paused.updateProgress()
        callback(currentStatus)
    }

    fun onDeleted() {
        //reset current progress
        currentProgress = Progress()
        currentStatus = deleted.updateProgress()

        callback(currentStatus)
    }

    private fun Status.updateProgress(): Status {
        progress = currentProgress
        return this
    }
}