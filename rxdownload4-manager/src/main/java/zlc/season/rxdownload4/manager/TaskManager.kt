package zlc.season.rxdownload4.manager

import android.app.NotificationManager
import android.content.Context
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable
import io.reactivex.flowables.ConnectableFlowable
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
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
        private val notificationCreator: NotificationCreator
) {

    init {
        notificationCreator.init(task)
    }

    private val notificationManager by lazy {
        clarityPotion.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val downloadHandler = StatusHandler()
    private val notificationHandler = StatusHandler {
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

    internal fun innerStart() {
        if (isStarted()) return

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
        notificationDisposable = connectFlowable.sample(250, MILLISECONDS)
                .doOnSubscribe { notificationHandler.onStarted() }
                .subscribeOn(mainThread())
                .observeOn(mainThread())
                .doOnNext { notificationHandler.onDownloading(it) }
                .doOnComplete { notificationHandler.onCompleted() }
                .doOnError { notificationHandler.onFailed(it) }
                .doOnCancel { notificationHandler.onPaused() }
                .subscribeBy()
    }

    internal fun innerStop() {
        if (isStopped()) return

        notificationDisposable.safeDispose()
        downloadDisposable.safeDispose()
        disposable.safeDispose()
    }

    internal fun innerDelete() {
        innerStop()

        task.delete(storage)
        notificationManager.cancel(task.hashCode())

        downloadHandler.onDeleted()
    }

    private fun isStarted(): Boolean {
        return disposable != null && !disposable!!.isDisposed
    }

    private fun isStopped(): Boolean {
        return disposable != null && disposable!!.isDisposed
    }
}