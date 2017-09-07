package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.schedulers.Schedulers.newThread
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.DB
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.core.DownloadConfig.STATUS_FACTORY
import zlc.season.rxdownload3.helper.ResponseUtil.contentLength
import zlc.season.rxdownload3.helper.ResponseUtil.fileName
import zlc.season.rxdownload3.helper.ResponseUtil.isSupportRange
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.http.HttpCore
import zlc.season.rxdownload3.status.Failed
import zlc.season.rxdownload3.status.Status
import java.util.concurrent.Semaphore


class RealMission(private val semaphore: Semaphore, val actual: Mission, val processor: FlowableProcessor<Status>) {

    private lateinit var maybe: Maybe<Any>

    var disposable: Disposable? = null

    init {
        create()
        configure()
        emitDownloadEvent(DB.readStatus(actual))
    }

    private fun create() {
        maybe = Maybe.just(ANY)
                .subscribeOn(io())
                .flatMap { check() }
                .flatMap { generateType() }
                .flatMap { it.download() }
                .observeOn(mainThread())
                .doOnDispose { emitFailedEvent(RuntimeException("Mission failed"), true) }
                .doOnError { emitFailedEvent(it) }
                .doOnSuccess { emitSuccessEvent() }
                .doFinally { semaphore.release() }
    }

    private fun emitSuccessEvent() {
        processor.onNext(STATUS_FACTORY.succeed())
    }

    private fun emitFailedEvent(throwable: Throwable, manualFlag: Boolean = false) {
        processor.onNext(STATUS_FACTORY.failed(throwable, manualFlag))
    }

    private fun emitWaitEvent() {
        processor.onNext(STATUS_FACTORY.waiting())
    }

    fun emitDownloadEvent(status: Status) {
        processor.onNext(status)
    }

    private fun configure() {
        processor
//                .sample(100, MILLISECONDS, true)
                .doOnNext {
                    DB.writeStatus(actual, it)
                    if (it is Failed) {

                    }
                }
    }

    fun setup(it: Response<Void>) {
        actual.savePath = if (actual.savePath.isEmpty()) DEFAULT_SAVE_PATH else actual.savePath
        actual.fileName = fileName(actual.fileName, actual.url, it)
        actual.rangeFlag = isSupportRange(it)
        actual.totalSize = contentLength(it)

    }

    private fun check(): Maybe<Any> {
        return if (actual.rangeFlag == null) {
            HttpCore.checkUrl(this)
        } else {
            Maybe.just(ANY)
        }
    }

    private fun generateType(): Maybe<DownloadType> {
        return if (actual.rangeFlag!!) {
            Maybe.just(RangeDownload(this))
        } else {
            Maybe.just(NormalDownload(this))
        }
    }

    fun start(): Maybe<Any> {
        return Maybe.create<Any> {
            emitWaitEvent()
            semaphore.acquire()
            disposable = maybe.subscribe()
            it.onSuccess(ANY)
        }.subscribeOn(newThread())
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