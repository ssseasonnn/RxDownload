package zlc.season.rxdownload3.core

import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.io
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.defaultSavePath
import zlc.season.rxdownload3.helper.*
import zlc.season.rxdownload3.http.HttpCore
import java.io.File
import java.util.concurrent.TimeUnit


class RealMission(val actual: Mission) {
    private val processor = BehaviorProcessor.create<Status>().toSerialized()
    private var status = Status().toSuspend()

    private var maybe = Maybe.empty<Any>()

    private var disposable: Disposable? = null
    private var downloadType: DownloadType? = null

    var totalSize = 0L

    private val notificationManager = DownloadConfig.context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val enableNotification = DownloadConfig.enableNotification
    private val notificationFactory = DownloadConfig.notificationFactory

    private val dbActor = DownloadConfig.dbActor

    init {
        initStatus()
        createMaybe()
    }

    private fun initStatus() {
        Maybe.just(ANY)
                .subscribeOn(io())
                .doOnSuccess {
                    if (dbActor.isExists(this)) {
                        dbActor.read(this)
                        downloadType = generateType()
                        downloadType!!.prepare()
                    }
                }
                .subscribe({
                    emitStatus(status)
                })
    }

    private fun createMaybe() {
        maybe = Maybe.just(ANY)
                .subscribeOn(io())
                .flatMap { check() }
                .flatMap { downloadType!!.download() }
                .doOnDispose { emitStatusWithNotification(status.toFailed()) }
                .doOnError {
                    loge("Download Error!", it)
                    emitStatusWithNotification(status.toFailed())
                }
                .doOnSuccess { emitStatusWithNotification(status.toSucceed()) }
                .doFinally { disposable = null }
    }

    fun getProcessor(): Flowable<Status> {
        return processor.debounce(1, TimeUnit.SECONDS)
                .onBackpressureLatest()
    }

    fun start(): Maybe<Any> {
        return Maybe.create<Any> {
            if (disposable != null && !disposable!!.isDisposed) {
                it.onSuccess(ANY)
                return@create
            }

            emitStatusWithNotification(status.toDownloading())

            disposable = maybe.subscribe()
            it.onSuccess(ANY)
        }.subscribeOn(Schedulers.newThread())
    }

    fun stop(): Maybe<Any> {
        return Maybe.create<Any> {
            if (disposable == null) {
                it.onSuccess(ANY)
                return@create
            }
            if (disposable!!.isDisposed) {
                it.onSuccess(ANY)
                return@create
            }

            dispose(disposable)
            disposable = null

            emitStatusWithNotification(status.toSuspend())

            it.onSuccess(ANY)
        }
    }

    fun getFile(): Maybe<File> {
        return Maybe
                .create<Any> {
                    dbActor.read(this)
                    it.onSuccess(ANY)
                }
                .subscribeOn(io())
                .flatMap {
                    val filePath = actual.savePath + File.separator + actual.saveName
                    val file = File(filePath)
                    if (file.exists()) {
                        Maybe.just(File(filePath))
                    } else {
                        Maybe.empty()
                    }
                }
    }

    fun emitStatusWithNotification(status: Status) {
        this.status = status
        processor.onNext(status)
        notifyNotification()
    }

    fun setup(it: Response<Void>) {
        actual.savePath = if (actual.savePath.isEmpty()) defaultSavePath else actual.savePath
        actual.saveName = fileName(actual.saveName, actual.url, it)
        actual.rangeFlag = isSupportRange(it)
        totalSize = contentLength(it)

        if (!dbActor.isExists(this)) {
            dbActor.create(this)
        }

        downloadType = generateType()
        downloadType!!.prepare()
        emitStatusWithNotification(status)
    }

    fun setStatus(status: Status) {
        this.status = status
    }

    private fun emitStatus(status: Status) {
        this.status = status
        processor.onNext(status)
    }

    private fun notifyNotification() {
        if (enableNotification) {
            notificationManager.notify(hashCode(),
                    notificationFactory.build(DownloadConfig.context, actual, status))
        }
    }

    private fun generateType(): DownloadType {
        return when {
            actual.rangeFlag == true -> RangeDownload(this)
            actual.rangeFlag == false -> NormalDownload(this)
            else -> {
                throw IllegalStateException("Mission range flag wrong")
            }
        }
    }

    private fun check(): Maybe<Any> {
        return if (actual.rangeFlag == null) {
            HttpCore.checkUrl(this)
        } else {
            Maybe.just(ANY)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RealMission

        if (actual != other.actual) return false

        return true
    }

    override fun hashCode(): Int {
        return actual.hashCode()
    }
}