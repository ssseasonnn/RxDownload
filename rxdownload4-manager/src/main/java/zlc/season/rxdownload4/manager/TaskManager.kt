package zlc.season.rxdownload4.manager

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.log
import zlc.season.rxdownload4.utils.safeDispose

class TaskManager(
        private val task: Task,
        private val flowable: Flowable<Progress>,
        private val processor: FlowableProcessor<Status>
) {
    private var normalStatus = Normal()
    private var startedStatus = Started()
    private var downloadStatus = Downloading()
    private var pausedStatus = Paused()
    private var completedStatus = Completed()
    private var failedStatus = Failed()

    private var currentStatus: Status = normalStatus
    private var currentProgress: Progress = Progress()
    private var disposable: Disposable? = null

    internal fun innerGet(): Flowable<Status> {
        return processor
    }

    internal fun innerStatus(): Status {
        return currentStatus
    }

    internal fun progress() = currentProgress

    @Synchronized
    internal fun innerStart() {
        if (isStarted()) return

        disposable = flowable.subscribeBy(
                onNext = {
                    "task downloading".log()
                    currentProgress = it
                    emitStatus(downloadStatus.apply { progress = it })
                },
                onComplete = {
                    "task completed".log()
                    emitStatus(completedStatus)
                },
                onError = {
                    "task failed".log()
                    emitStatus(failedStatus.apply { throwable = it })
                }
        )
        "task started".log()
        emitStatus(startedStatus)
    }

    @Synchronized
    internal fun innerStop() {
        if (isStopped()) return

        disposable.safeDispose()
        emitStatus(pausedStatus)

        "task paused".log()
    }

    private fun isStarted(): Boolean {
        return disposable != null && !disposable!!.isDisposed
    }

    private fun isStopped(): Boolean {
        return disposable != null && disposable!!.isDisposed
    }


    private fun emitStatus(status: Status) {
        currentStatus = status
        processor.onNext(currentStatus)
    }
}