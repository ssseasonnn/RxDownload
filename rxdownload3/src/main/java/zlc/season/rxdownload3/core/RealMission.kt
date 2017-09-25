package zlc.season.rxdownload3.core

import android.app.NotificationManager
import android.content.Context
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.schedulers.Schedulers.newThread
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.defaultSavePath
import zlc.season.rxdownload3.database.DbActor
import zlc.season.rxdownload3.helper.contentLength
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.helper.fileName
import zlc.season.rxdownload3.helper.isSupportRange
import zlc.season.rxdownload3.http.HttpCore
import zlc.season.rxdownload3.notification.NotificationFactory
import java.io.File
import java.util.concurrent.TimeUnit


class RealMission(val actual: Mission) {
    private val processor = BehaviorProcessor.create<Status>().toSerialized()
    private var status = Status().toSuspend()

    private lateinit var maybe: Maybe<Any>
    private var downloadType: DownloadType? = null

    private var disposable: Disposable? = null

    var totalSize = 0L

    private val enableNotification = DownloadConfig.enableNotification
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationFactory: NotificationFactory

    private val enableDb = DownloadConfig.enableDb
    private lateinit var dbActor: DbActor

    init {
        if (enableNotification) {
            notificationManager = DownloadConfig.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationFactory = DownloadConfig.notificationFactory
        }

        if (enableDb) {
            dbActor = DownloadConfig.dbActor
        }

        createMaybe()
        initStatus()
    }

    private fun initStatus() {
        Maybe.create<Any> {
            readFromDb()

            downloadType = generateType()
            downloadType?.initStatus(true)

            it.onSuccess(ANY)
        }.subscribeOn(io()).subscribe({
            emitStatus(status)
        })
    }

    private fun createMaybe() {
        maybe = Maybe.just(ANY)
                .subscribeOn(io())
                .flatMap { check() }
                .flatMap { download() }
                .doOnDispose { emitStatusWithNotification(status.toSuspend()) }
                .doOnError { emitStatusWithNotification(status.toFailed()) }
                .doOnSuccess { emitStatusWithNotification(status.toSucceed()) }
                .doFinally { disposable = null }
    }

    fun getFlowable(): Flowable<Status> {
        return processor.sample(30, TimeUnit.MILLISECONDS)
                .onBackpressureLatest()
    }

    fun start(): Maybe<Any> {
        return Maybe.create<Any> {
            if (disposable != null) {
                it.onSuccess(ANY)
                return@create
            }
            emitStatusWithNotification(status.toWaiting())
            disposable = maybe.subscribe()

            it.onSuccess(ANY)
        }.subscribeOn(Schedulers.newThread())
    }

    fun stop(): Maybe<Any> {
        return Maybe.create<Any> {
            dispose(disposable)
            disposable = null

            it.onSuccess(ANY)
        }
    }

    fun getFile(): Maybe<File> {
        return Maybe.create<File> {
            readFromDb()
            if (actual.saveName.isEmpty()) {
                it.onError(Throwable("Save Name is empty!"))
                return@create
            }

            var path = actual.savePath
            if (path.isEmpty()) {
                path = DownloadConfig.defaultSavePath
            }
            val file = File(path + File.separator + actual.saveName)
            if (file.exists()) {
                it.onSuccess(file)
                return@create
            }

            it.onError(Throwable("No such file"))
        }.subscribeOn(newThread())
    }

    fun setup(resp: Response<Void>) {
        actual.savePath = if (actual.savePath.isEmpty()) defaultSavePath else actual.savePath
        actual.saveName = fileName(actual.saveName, actual.url, resp)
        actual.rangeFlag = isSupportRange(resp)
        totalSize = contentLength(resp)

        insertToDb()

        downloadType = generateType()
        downloadType?.initStatus(false)
    }

    private fun readFromDb() {
        if (enableDb) {
            if (dbActor.isExists(this)) {
                dbActor.read(this)
            }
        }
    }

    private fun insertToDb() {
        if (enableDb) {
            if (!dbActor.isExists(this)) {
                dbActor.create(this)
            }
        }
    }

    fun setStatus(status: Status) {
        this.status = status
    }

    fun emitStatusWithNotification(status: Status) {
        this.status = status
        processor.onNext(status)
        notifyNotification()
    }

    fun emitStatus(status: Status) {
        this.status = status
        processor.onNext(status)
    }

    private fun notifyNotification() {
        if (enableNotification) {
            notificationManager.notify(hashCode(),
                    notificationFactory.build(DownloadConfig.context, actual, status))
        }
    }

    private fun generateType(): DownloadType? {
        return when {
            actual.rangeFlag == true -> RangeDownload(this)
            actual.rangeFlag == false -> NormalDownload(this)
            else -> null
        }
    }

    private fun check(): Maybe<Any> {
        return if (actual.rangeFlag == null) {
            HttpCore.checkUrl(this)
        } else {
            Maybe.just(ANY)
        }
    }

    private fun download(): Maybe<Any> {
        return downloadType!!.download()
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