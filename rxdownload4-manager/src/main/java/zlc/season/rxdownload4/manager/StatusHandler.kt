package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.log

class StatusHandler(
        private val task: Task,
        private val enableLog: Boolean = false,
        var callback: (Status) -> Unit = {}
) {

    var currentStatus: Status = Normal()
    var currentProgress: Progress = Progress()

    private val normal = Normal()
    private val started = Started()
    private val downloading = Downloading()
    private val paused = Paused()
    private val completed = Completed()
    private val failed = Failed()

    fun onNormal() {
        currentStatus = normal.apply { progress = currentProgress }
        callback(currentStatus)

        if (enableLog) "[${task.tag()}] normal".log()
    }

    fun onStarted() {
        currentStatus = started.apply { progress = currentProgress }
        callback(currentStatus)

        if (enableLog) "[${task.tag()}] started".log()
    }

    fun onDownloading(next: Progress) {
        currentProgress = next
        currentStatus = downloading.apply { progress = currentProgress }
        callback(currentStatus)

        if (enableLog) "[${task.tag()}] downloading ${next.percentStr()}".log()
    }

    fun onCompleted() {
        currentStatus = completed.apply { progress = currentProgress }
        callback(currentStatus)

        if (enableLog) "[${task.tag()}] completed".log()
    }

    fun onFailed(t: Throwable) {
        currentStatus = failed.apply {
            progress = currentProgress
            throwable = t
        }
        callback(currentStatus)

        if (enableLog) "[${task.tag()}] failed".log()
    }

    fun onPaused() {
        currentStatus = paused.apply { progress = currentProgress }
        callback(currentStatus)

        if (enableLog) "[${task.tag()}] paused".log()
    }
}