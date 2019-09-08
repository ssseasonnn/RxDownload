package zlc.season.rxdownload4.manager

import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable
import io.reactivex.flowables.ConnectableFlowable
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.delete
import zlc.season.rxdownload4.file
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.safeDispose
import java.util.concurrent.TimeUnit.MILLISECONDS

class TaskManager(
        private val task: Task,
        private val storage: Storage,
        private val connectFlowable: ConnectableFlowable<Progress>,
        private val notificationCreator: NotificationCreator,
        taskRecorder: TaskRecorder
) {

    init {
        notificationCreator.init(task)
    }

    private val downloadHandler by lazy { StatusHandler(task, taskRecorder) }

    private val notificationHandler by lazy {
        StatusHandler(task, logTag = "Notification") {
            val notification = notificationCreator.create(task, it)
            showNotification(task, notification)
        }
    }

    //Download disposable
    private var disposable: Disposable? = null
    private var downloadDisposable: Disposable? = null
    private var notificationDisposable: Disposable? = null


    internal fun addCallback(tag: Any, callback: (Status) -> Unit) {
        downloadHandler.addCallback(tag, callback)
    }

    internal fun removeCallback(tag: Any) {
        downloadHandler.removeCallback(tag)
    }

    internal fun currentStatus() = downloadHandler.currentStatus

    internal fun getFile() = task.file(storage)

    internal fun innerStart() {
        if (isStarted()) {
            return
        }

        subscribeNotification()

        subscribeDownload()

        disposable = connectFlowable.connect()
    }

    private fun subscribeDownload() {
        downloadDisposable = connectFlowable
                .doOnSubscribe { downloadHandler.onStarted() }
                .subscribeOn(mainThread())
                .observeOn(mainThread())
                .doOnNext { downloadHandler.onDownloading(it) }
                .doOnComplete { downloadHandler.onCompleted() }
                .doOnError { downloadHandler.onFailed(it) }
                .doOnCancel { downloadHandler.onPaused() }
                .subscribeBy()
    }

    private fun subscribeNotification() {
        notificationDisposable = connectFlowable.sample(500, MILLISECONDS)
                .doOnSubscribe { notificationHandler.onStarted() }
                .doOnNext { notificationHandler.onDownloading(it) }
                .doOnComplete { notificationHandler.onCompleted() }
                .doOnError { notificationHandler.onFailed(it) }
                .doOnCancel { notificationHandler.onPaused() }
                .subscribeBy()
    }

    internal fun innerStop() {
        if (isStopped()) {
            //fix notification update too fast bug
            notificationHandler.onPaused()
            downloadHandler.onPaused()
            return
        }

        notificationDisposable.safeDispose()
        downloadDisposable.safeDispose()
        disposable.safeDispose()

        //fix when app killed notification can't stop bug
        if (disposable == null) {
            notificationHandler.onPaused()
        }
    }

    internal fun innerDelete() {
        innerStop()

        task.delete(storage)

        //special handle
        downloadHandler.onDeleted()

        cancelNotification(task)
    }

    private fun isStarted(): Boolean {
        return disposable != null && !disposable!!.isDisposed
    }

    private fun isStopped(): Boolean {
        return disposable != null && disposable!!.isDisposed
    }
}