package zlc.season.rxdownload4.manager

import android.annotation.SuppressLint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable
import io.reactivex.flowables.ConnectableFlowable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subscribers.DisposableSubscriber
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.delete
import zlc.season.rxdownload4.file
import zlc.season.rxdownload4.manager.notification.NotificationCreator
import zlc.season.rxdownload4.manager.notification.notificationManager
import zlc.season.rxdownload4.storage.Storage
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.safeDispose
import java.util.concurrent.TimeUnit.MILLISECONDS

class TaskManager(
        private val task: Task,
        private val storage: Storage,

        private val connectFlowable: ConnectableFlowable<Progress>,
        private val notificationCreator: NotificationCreator
) {

    init {
        notificationCreator.init(task)
    }

    private val downloadHandler = StatusHandler(task, true)
    private val notificationHandler = StatusHandler(task) {
        val notification = notificationCreator.create(task, it)
        notification?.let {
            notificationManager.notify(task.hashCode(), it)
        }
    }

    //Download disposable
    private var disposable: Disposable? = null
    private var downloadDisposable: Disposable? = null
    private var notificationDisposable: Disposable? = null

    fun setCallback(callback: (Status) -> Unit = {}) {
        downloadHandler.callback = callback
    }

    internal fun currentStatus() = downloadHandler.currentStatus

    internal fun getFile() = task.file(storage)

    @SuppressLint("CheckResult")
    @Synchronized
    internal fun innerStart() {
        if (isStarted()) return

        subscribeNotification()

        subscribeDownload()

        disposable = connectFlowable.connect()
    }

    private fun subscribeDownload() {
        downloadDisposable = connectFlowable
                .doOnSubscribe {
                    downloadHandler.onStarted()
                }
                .subscribeOn(mainThread())
                .observeOn(mainThread())
                .doOnCancel {
                    downloadHandler.onPaused()
                }
                .doOnNext {
                    downloadHandler.onDownloading(it)
                }
                .doOnComplete {
                    downloadHandler.onCompleted()
                }
                .doOnError {
                    downloadHandler.onFailed(it)
                }
                .subscribeBy()
    }

    private fun subscribeNotification() {
        notificationDisposable = connectFlowable.sample(250, MILLISECONDS)
                .doOnSubscribe {
                    notificationHandler.onStarted()
                }
                .subscribeOn(mainThread())
                .observeOn(mainThread())
                .doOnCancel {
                    notificationHandler.onPaused()
                }
                .doOnNext {
                    notificationHandler.onDownloading(it)
                }
                .doOnComplete {
                    notificationHandler.onCompleted()
                }
                .doOnError {
                    notificationHandler.onFailed(it)
                }
                .subscribeBy()
    }

    @Synchronized
    internal fun innerStop() {
        if (isStopped()) return

        notificationDisposable.safeDispose()
        downloadDisposable.safeDispose()
        disposable.safeDispose()
    }

    internal fun innerDelete() {
        task.delete(storage)
        notificationManager.cancel(task.hashCode())
    }

    private fun isStarted(): Boolean {
        return disposable != null && !disposable!!.isDisposed
    }

    private fun isStopped(): Boolean {
        return disposable != null && disposable!!.isDisposed
    }
}