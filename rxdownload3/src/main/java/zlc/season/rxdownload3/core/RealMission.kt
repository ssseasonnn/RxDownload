package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.schedulers.Schedulers.newThread
import zlc.season.rxdownload3.core.DownloadConfig.DB
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.core.DownloadConfig.FACTORY
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.http.HttpCore
import zlc.season.rxdownload3.status.Failed
import zlc.season.rxdownload3.status.Status
import java.util.concurrent.Semaphore


class RealMission(private val semaphore: Semaphore, val mission: Mission, val processor: FlowableProcessor<Status>) {
    var isSupportRange: Boolean = false
    var isFileChange: Boolean = false

    var lastModify: Long = 0L

    var contentLength: Long = -1L

    var realFileName = mission.fileName
    var realPath = if (mission.savePath.isEmpty()) DEFAULT_SAVE_PATH else mission.savePath

    var disposable: Disposable? = null

    private lateinit var maybe: Maybe<Any>
    private val ANY = Any()

    init {
        create()
        configure()
    }

    private fun create() {
        maybe = Maybe.just(ANY)
                .flatMap { HttpCore.checkUrl(this) }
                .flatMap { DownloadType.generateType(this) }
                .flatMap { it.download() }
                .doOnDispose { processor.onNext(FACTORY.failed(MissionStoppedException())) }
                .doOnError { processor.onNext(FACTORY.failed(MissionStoppedException(it))) }
                .doOnSuccess {
                    processor.onNext(FACTORY.succeed())
                    MissionBox.remove(this)
                }
                .doFinally {
                    disposable = null
                    semaphore.release()
                }
    }

    private fun configure() {
        processor.onNext(DB.readStatus(mission))

        processor
//                .sample(100, MILLISECONDS, true)
                .doOnNext {
                    DB.writeStatus(mission, it)
                    if (it is Failed) {

                    }
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

        if (mission != other.mission) return false

        return true
    }

    override fun hashCode(): Int {
        return mission.hashCode()
    }
}