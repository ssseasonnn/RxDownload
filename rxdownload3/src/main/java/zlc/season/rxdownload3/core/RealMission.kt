package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.schedulers.Schedulers.newThread
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.DB
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.core.DownloadConfig.FACTORY
import zlc.season.rxdownload3.helper.ResponseUtil
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
    }

    private fun create() {
        maybe = Maybe.just(ANY)
                .flatMap {
                    if (check()) {
                        HttpCore.checkUrl(this)
                    } else {
                        Maybe.just(it)
                    }
                }
                .flatMap { generateType() }
                .flatMap { it.download() }
                .doOnDispose { processor.onNext(FACTORY.failed(MissionStoppedException())) }
                .doOnError { processor.onNext(FACTORY.failed(MissionStoppedException(it))) }
                .doOnSuccess {
                    processor.onNext(FACTORY.succeed())
                }
                .doFinally {
                    disposable = null
                    semaphore.release()
                }
    }

    private fun configure() {
        processor.onNext(DB.readStatus(actual))

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
        actual.fileName = ResponseUtil.fileName(actual.fileName, actual.url, it)
        actual.rangeFlag = ResponseUtil.isSupportRange(it)
        actual.totalSize = ResponseUtil.contentLength(it)

    }

    private fun check(): Boolean {
        return actual.rangeFlag == null
    }

    private fun generateType(): Maybe<DownloadType> {
        return if (actual.rangeFlag!!) {
            Maybe.just(RangeDownload(this))
        } else {
            Maybe.just(NormalDownload(this))
        }
    }

    fun start(): Maybe<Any> {
//        if (disposable != null) {
//            return Maybe.error(RuntimeException("Mission already started"))
//        }


        return Maybe
                .create<Any> {
                    processor.onNext(FACTORY.waiting())
                    semaphore.acquire()
                    disposable = maybe.subscribe()
                    it.onSuccess(ANY)
                    it.onComplete()
                }
                .subscribeOn(newThread())
                .doOnError {
                    dispose(disposable)
                    disposable = null
                    semaphore.release()
                }
    }

    fun stop(): Maybe<Any> {
//        if (disposable == null) {
//            return Maybe.error(RuntimeException("Mission has not started"))
//        }

        return Maybe.create<Any> {
            dispose(disposable)
            disposable = null
            semaphore.release()
            it.onSuccess(ANY)
            it.onComplete()
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