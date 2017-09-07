package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.schedulers.Schedulers.newThread
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.DB
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.helper.ResponseUtil.contentLength
import zlc.season.rxdownload3.helper.ResponseUtil.fileName
import zlc.season.rxdownload3.helper.ResponseUtil.isSupportRange
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.http.HttpCore
import java.util.concurrent.Semaphore


class RealMission(private val semaphore: Semaphore, val actual: Mission) {
    private val processor = BehaviorProcessor.create<Status>().toSerialized()

    lateinit var maybe: Maybe<Any>

    var disposable: Disposable? = null
    var totalSize = 0L
    var status: Status = DB.readStatus(actual)

    init {
        create()
        configure()
    }

    fun getProcessor(): Flowable<Status> {
        emitStatus(status)
        return processor.onBackpressureLatest()
    }

    private fun create() {
        maybe = Maybe.just(ANY)
                .subscribeOn(io())
                .flatMap { check() }
                .flatMap { generateType() }
                .flatMap { it.download() }
                .observeOn(mainThread())
                .doOnDispose { emitStatus(Failed(status, RuntimeException("Mission failed"), true)) }
                .doOnError { emitStatus(Failed(status, it)) }
                .doOnSuccess { emitStatus(Succeed(status)) }
                .doFinally { semaphore.release() }
    }

    fun emitStatus(status: Status) {
        this.status = status
        processor.onNext(status)
    }

    fun emitStatus() {
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
        totalSize = contentLength(it)

    }

    private fun check(): Maybe<Any> {
        return if (actual.rangeFlag == null || actual.forceReDownload) {
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
            if (disposable != null && !disposable!!.isDisposed) {
                it.onSuccess(ANY)
                return@create
            }

            emitStatus(Waiting(status))
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