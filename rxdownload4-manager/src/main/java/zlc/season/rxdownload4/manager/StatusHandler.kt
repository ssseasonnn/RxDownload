package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.Progress

class StatusHandler(var callback: (Status) -> Unit = {}) {
    private val normal = Normal()
    private val started = Started()
    private val downloading = Downloading()
    private val paused = Paused()
    private val completed = Completed()
    private val failed = Failed()

    var currentStatus: Status = normal
    private var currentProgress: Progress = Progress()

    fun onNormal() {
        //reset current progress
        currentProgress = Progress()

        currentStatus = normal.apply { progress = currentProgress }
        callback(currentStatus)
    }

    fun onStarted() {
        currentStatus = started.apply { progress = currentProgress }
        callback(currentStatus)
    }

    fun onDownloading(next: Progress) {
        //set current progress
        currentProgress = next

        currentStatus = downloading.apply { progress = currentProgress }
        callback(currentStatus)
    }

    fun onCompleted() {
        currentStatus = completed.apply { progress = currentProgress }
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
        currentStatus = paused.apply { progress = currentProgress }
        callback(currentStatus)
    }
}