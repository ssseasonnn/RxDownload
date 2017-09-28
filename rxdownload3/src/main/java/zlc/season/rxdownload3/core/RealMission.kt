package zlc.season.rxdownload3.core

import android.app.NotificationManager
import android.content.Context
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.schedulers.Schedulers.newThread
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.defaultSavePath
import zlc.season.rxdownload3.database.DbActor
import zlc.season.rxdownload3.extension.Extension
import zlc.season.rxdownload3.helper.*
import zlc.season.rxdownload3.http.HttpCore
import zlc.season.rxdownload3.notification.NotificationFactory
import java.io.File
import java.util.concurrent.TimeUnit.MILLISECONDS


class RealMission(val actual: Mission) {
    var totalSize = 0L
    var status: Status = Suspend(Status())

    private val processor = BehaviorProcessor.create<Status>().toSerialized()

    private var disposable: Disposable? = null
    private var downloadType: DownloadType? = null

    private lateinit var initMaybe: Maybe<Any>
    private lateinit var downloadFlowable: Flowable<Status>

    private val enableNotification = DownloadConfig.enableNotification
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationFactory: NotificationFactory

    private val enableDb = DownloadConfig.enableDb
    private lateinit var dbActor: DbActor

    private val extensions = mutableListOf<Extension>()

    init {
        init()
    }

    private fun init() {
        initMaybe = Maybe.create<Any> {
            loadConfig()
            createFlowable()
            initMission()
            initExtension()
            initStatus()

            it.onSuccess(ANY)
        }.subscribeOn(newThread())

        initMaybe.subscribe({ emitStatus(status) })
    }

    private fun loadConfig() {
        if (enableNotification) {
            notificationManager = DownloadConfig.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationFactory = DownloadConfig.notificationFactory
        }

        if (enableDb) {
            dbActor = DownloadConfig.dbActor
        }
    }

    private fun initMission() {
        if (enableDb) {
            if (dbActor.isExists(this)) {
                dbActor.read(this)
            } else {
                dbActor.create(this)
            }
        }
    }

    private fun initExtension() {
        val ext = DownloadConfig.extensions
        ext.mapTo(extensions) { it.newInstance() }
        extensions.forEach { it.init(this) }
    }

    private fun initStatus() {
        downloadType = generateType()
        downloadType?.initStatus()
    }

    private fun createFlowable() {
        downloadFlowable = Flowable.just(ANY)
                .subscribeOn(io())
                .doOnSubscribe { emitStatusWithNotification(Waiting(status)) }
                .flatMap { checkAndDownload() }
                .doOnError { emitStatusWithNotification(Failed(status)) }
                .doOnComplete { emitStatusWithNotification(Succeed(status)) }
                .doOnCancel { emitStatusWithNotification(Suspend(status)) }
                .doFinally { disposable = null }
    }

    fun findExtension(extension: Class<out Extension>): Extension {
        return extensions.first { extension.isInstance(it) }
    }

    fun getFlowable(): Flowable<Status> {
        return processor.sample(30, MILLISECONDS, true)
                .onBackpressureLatest()
    }

    fun start(): Maybe<Any> {
        return Maybe.create<Any> {
            if (disposable == null) {
                disposable = downloadFlowable.subscribe({ emitStatusWithNotification(it) },
                        { loge("Download failed", it) })
            }
            it.onSuccess(ANY)
        }.subscribeOn(newThread())
    }

    fun stop(): Maybe<Any> {
        return Maybe.create<Any> {
            dispose(disposable)
            disposable = null

            it.onSuccess(ANY)
        }.subscribeOn(newThread())
    }

    fun getFile(): File? {
        return downloadType?.getFile()
    }

    fun file(): Maybe<File> {
        return Maybe.create<File> {
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

        downloadType = generateType()

        if (enableDb) {
            dbActor.update(this)
        }
    }

    fun emitStatusWithNotification(status: Status) {
        emitStatus(status)
        notifyNotification()
    }

    fun emitStatus(status: Status) {
        this.status = status
        processor.onNext(status)
        if (enableDb) {
            dbActor.update(this)
        }
    }

    private fun notifyNotification() {
        if (enableNotification) {
            notificationManager.notify(hashCode(),
                    notificationFactory.build(DownloadConfig.context, this, status))
        }
    }

    private fun generateType(): DownloadType? {
        return when {
            actual.rangeFlag == true -> RangeDownload(this)
            actual.rangeFlag == false -> NormalDownload(this)
            else -> null
        }
    }

    private fun checkAndDownload(): Flowable<Status> {
        return check().flatMapPublisher { download() }
    }

    private fun check(): Maybe<Any> {
        return if (actual.rangeFlag == null) {
            HttpCore.checkUrl(this)
        } else {
            Maybe.just(ANY)
        }
    }

    private fun download(): Flowable<Status> {
        return downloadType?.download() ?:
                Flowable.error(IllegalStateException("Illegal download type"))
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