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
        handleCallback()
    }

    fun onStarted() {
        currentStatus = started.updateProgress()
        handleCallback()
    }

    fun onDownloading(next: Progress) {
        //set current progress
        currentProgress = next

        currentStatus = downloading.updateProgress()
        handleCallback()
    }

    fun onCompleted() {
        currentStatus = completed.updateProgress()
        handleCallback()
    }

    fun onFailed(t: Throwable) {
        currentStatus = failed.apply {
            progress = currentProgress
            throwable = t
        }
        handleCallback()
    }

    fun onPaused() {
        currentStatus = paused.updateProgress()

        handleCallback()
    }

    fun onDeleted() {
        //reset current progress
        currentProgress = Progress()
        currentStatus = deleted.updateProgress()

        handleCallback()
    }

    private fun Status.updateProgress(): Status {
        progress = currentProgress
        return this
    }

    private fun handleCallback() {
        callback(currentStatus)
    }
}