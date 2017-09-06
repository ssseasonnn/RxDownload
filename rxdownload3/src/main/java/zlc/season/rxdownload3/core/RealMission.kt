package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.schedulers.Schedulers.newThread
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.http.HttpCore
import zlc.season.rxdownload3.status.DownloadStatus
import java.util.concurrent.Semaphore


class RealMission(val semaphore: Semaphore, val mission: Mission, val processor: FlowableProcessor<DownloadStatus>) {
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
                .doOnDispose { processor.onError(MissionStoppedException()) }
                .doOnError { processor.onError(MissionStoppedException(it)) }
                .doOnSuccess { processor.onComplete() }
                .doFinally {
                    disposable = null
                    MissionBox.remove(this)
                    semaphore.release()
                }
    }

    private fun configure() {
        processor.onNext(DownloadConfig.DB.readStatus(mission))

        processor.doOnNext { DownloadConfig.DB.writeStatus(mission, it) }
        processor.doOnError { DownloadConfig.DB.missionFailed(mission) }
        processor.doOnComplete { DownloadConfig.DB.missionSuccess(mission) }
    }

    fun start(): Maybe<Any> {
        if (disposable != null) {
            return Maybe.error(RuntimeException("Mission already started"))
        }

        return Maybe
                .create<Any> {
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
        if (disposable == null) {
            return Maybe.error(RuntimeException("Mission has not started"))
        }

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