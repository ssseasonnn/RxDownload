package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.io
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.defaultSavePath
import zlc.season.rxdownload3.helper.contentLength
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.helper.fileName
import zlc.season.rxdownload3.helper.isSupportRange
import zlc.season.rxdownload3.http.HttpCore
import java.util.concurrent.Semaphore


class RealMission(private val semaphore: Semaphore, val actual: Mission) {
    private val processor = BehaviorProcessor.create<Status>().toSerialized()
    private var status = Status().toSuspend()

    private var maybe = Maybe.empty<Any>()

    private var disposable: Disposable? = null

    var totalSize = 0L

    init {
        initStatus()
        createMaybe()
    }

    private fun initStatus() {
        Maybe.create<Any> {
            DownloadConfig.dbActor.read(this)
            it.onSuccess(ANY)
        }.doOnSuccess {
            if (actual.rangeFlag != null) {
                val type = createType()
                type.initStatus()
            }
        }.subscribeOn(io()).subscribe({
            emitStatus(status)
        })
    }

    private fun createMaybe() {
        maybe = Maybe.just(ANY)
                .subscribeOn(io())
                .flatMap { check() }
                .flatMap {
                    HttpCore.checkUrl(this)
                }
                .doOnDispose { emitStatus(status.toFailed()) }
                .doOnError { emitStatus(status.toFailed()) }
                .doOnSuccess { emitStatus(status.toSucceed()) }
                .doFinally {
                    semaphore.release()
                    disposable = null
                }
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

    fun setStatus(status: Status) {
        this.status = status
    }

    fun emitStatus(status: Status) {
        this.status = status
        processor.onNext(status)
    }

    fun setup(it: Response<Void>) {
        actual.savePath = if (actual.savePath.isEmpty()) defaultSavePath else actual.savePath
        actual.saveName = fileName(actual.saveName, actual.url, it)
        actual.rangeFlag = isSupportRange(it)
        totalSize = contentLength(it)

        if (!DownloadConfig.dbActor.isExists(this)) {
            DownloadConfig.dbActor.create(this)
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