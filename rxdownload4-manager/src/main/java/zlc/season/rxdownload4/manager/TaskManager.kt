package zlc.season.rxdownload4.manager

import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.delete
import zlc.season.rxdownload4.file
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.log
import zlc.season.rxdownload4.utils.safeDispose

class TaskManager(
        private val task: Task,
        private val storage: Storage,

        private val flowable: Flowable<Progress>
) {
    //Status objects
    private val normal = Normal()
    private val started = Started()
    private val downloading = Downloading()
    private val paused = Paused()
    private val completed = Completed()
    private val failed = Failed()

    //Current status
    private var currentStatus: Status = normal
    private var currentProgress: Progress = Progress()

    //Download disposable
    private var disposable: Disposable? = null
    private var onNext: (Status) -> Unit = {}

    internal fun setOnNext(onNext: (Status) -> Unit = {}) {
        this.onNext = onNext
    }

    internal fun getFile() = task.file(storage)

    internal fun innerDelete() = task.delete(storage)

    internal fun innerStatus() = currentStatus

    @Synchronized
    internal fun innerStart() {
        if (isStarted()) return

        disposable = flowable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = {
                            "[${task.tag()}] downloading ${it.percentStr()}".log()
                            currentProgress = it
                            emitStatus(downloading)
                        },
                        onComplete = {
                            "[${task.tag()}] completed".log()
                            emitStatus(completed)
                        },
                        onError = {
                            "[${task.tag()}] failed".log()
                            emitStatus(failed.apply { throwable = it })
                        }
                )
        "[${task.tag()}] started".log()
        emitStatus(started)
    }

    @Synchronized
    internal fun innerStop() {
        if (isStopped()) return

        disposable.safeDispose()
        emitStatus(paused)

        "[${task.tag()}] paused".log()
    }

    private fun isStarted(): Boolean {
        return disposable != null && !disposable!!.isDisposed
    }

    private fun isStopped(): Boolean {
        return disposable != null && disposable!!.isDisposed
    }


    private fun emitStatus(status: Status) {
        currentStatus = status
        currentStatus.progress = currentProgress

        onNext(currentStatus)
    }
}