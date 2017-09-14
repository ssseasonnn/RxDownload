package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.io
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.helper.contentLength
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.helper.fileName
import zlc.season.rxdownload3.helper.isSupportRange
import zlc.season.rxdownload3.http.HttpCore
import java.util.concurrent.Semaphore


class RealMission(private val semaphore: Semaphore, val actual: Mission) {
    private val processor = BehaviorProcessor.create<Status>().toSerialized()
    private var status: Status = Status(0, 0).toSuspend()

    lateinit var maybe: Maybe<Any>

    private var disposable: Disposable? = null

    var totalSize = 0L

    init {
        createMaybe()
        initStatus()
    }

    private fun initStatus() {
        Maybe.create<Any> {
            DownloadConfig.DB.read(actual)
            if (actual.rangeFlag == null) {
                it.onSuccess(ANY)
            } else {
                val type = createType()
                type.initStatus()
                it.onSuccess(ANY)
            }
        }.subscribeOn(io()).subscribe()
    }

    private fun createMaybe() {
        maybe = Maybe.just(ANY)
                .subscribeOn(io())
                .flatMap {
                    HttpCore.checkUrl(this)
                }
                .flatMap {
                    val type = createType()
                    type.download()
                }
                .doOnDispose { emitStatus(status.toFailed()) }
                .doOnError { emitStatus(status.toFailed()) }
                .doOnSuccess { emitStatus(status.toSucceed()) }
                .doFinally { semaphore.release() }
    }

    fun getProcessor(): Flowable<Status> {
        return processor.onBackpressureLatest()
    }

    fun start(): Maybe<Any> {
        return Maybe.create<Any> {
            if (disposable != null && !disposable!!.isDisposed) {
                it.onSuccess(ANY)
                return@create
            }

            emitStatus(status.toWaiting())
            semaphore.acquire()
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
            semaphore.release()
            it.onSuccess(ANY)
        }
    }

    fun emitStatus(status: Status) {
        this.status = status
        processor.onNext(status)
    }

    fun setup(it: Response<Void>) {
        actual.savePath = if (actual.savePath.isEmpty()) DEFAULT_SAVE_PATH else actual.savePath
        actual.saveName = fileName(actual.saveName, actual.url, it)
        actual.rangeFlag = isSupportRange(it)
        totalSize = contentLength(it)

        if (!DownloadConfig.DB.isExists(actual)) {
            DownloadConfig.DB.create(actual)
        }
    }

    private fun createType(): DownloadType {
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