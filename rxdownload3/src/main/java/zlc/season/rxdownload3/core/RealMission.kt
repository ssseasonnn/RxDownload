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
import zlc.season.rxdownload3.extension.Extension
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
    private var status: Status = Suspend(Status())

    private lateinit var maybe: Maybe<Any>
    private var downloadType: DownloadType? = null

    private var disposable: Disposable? = null

    var totalSize = 0L

    private val enableNotification = DownloadConfig.enableNotification
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationFactory: NotificationFactory

    private val enableDb = DownloadConfig.enableDb
    private lateinit var dbActor: DbActor

    private val extensions = mutableListOf<Extension>()

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
        initExtension()
    }

    private fun initExtension() {
        val ext = DownloadConfig.extensions
        ext.mapTo(extensions) { it.newInstance() }
        extensions.forEach { it.init(this) }
    }

    private fun initStatus() {
        readFromDb()
        downloadType = generateType()
        downloadType?.initStatus(true)
        emitStatus(status)
    }

    private fun createMaybe() {
        maybe = Maybe.just(ANY)
                .subscribeOn(io())
                .flatMap { check() }
                .flatMap { download() }
                .doOnDispose { emitStatusWithNotification(Suspend(status)) }
                .doOnError { emitStatusWithNotification(Failed(status)) }
                .doOnSuccess { emitStatusWithNotification(Succeed(status)) }
                .doFinally { disposable = null }
    }

    fun findExtension(extension: Class<out Extension>): Extension {
        return extensions.first { extension.isInstance(it) }
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
            emitStatusWithNotification(Waiting(status))
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

    fun getFile(): File? {
        if (actual.saveName.isEmpty()) {
            return null
        }

        var path = actual.savePath
        if (path.isEmpty()) {
            path = DownloadConfig.defaultSavePath
        }
        val file = File(path + File.separator + actual.saveName)
        if (file.exists()) {
            return file
        }

        return null
    }

    fun file(): Maybe<File> {
        return Maybe.create<File> {
            readFromDb()
            val file = getFile()
            if (file == null) {
                it.onError(RuntimeException("No such file"))
            } else {
                it.onSuccess(file)
            }
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

    fun getStatus(): Status {
        return status
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