package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.http.HttpProcessor
import java.util.concurrent.Semaphore


class RealMission(val semaphore: Semaphore, val mission: Mission, val processor: FlowableProcessor<DownloadStatus>) {
    var isStopped: Boolean = false

    var isSupportRange: Boolean = false
    var isFileChange: Boolean = false

    var lastModify: Long = 0L

    var contentLength: Long = -1L

    var realFileName = mission.fileName
    var realPath = if (mission.savePath.isEmpty()) DEFAULT_SAVE_PATH else mission.savePath

    var disposable: Disposable? = null

    var maybe: Maybe<Any>? = null

    init {
        processor.doOnLifecycle({ s ->

        }, {

        }, {

        })
        processor.onNext(DownloadStatus(0))
    }


    fun create() {
        maybe = Maybe.create<Any> { maybe(it) }
                .flatMap { HttpProcessor.checkUrl(this) }
                .flatMap { DownloadType.generateType(this) }
                .flatMap { it.download() }
                .doOnDispose { processor.onError(MissionStoppedException()) }
                .doOnError { processor.onError(it) }
                .doOnSuccess { processor.onComplete() }
                .doFinally {
                    MissionBox.remove(this)
                    semaphore.release()
                }

    }

    private fun maybe(it: MaybeEmitter<Any>) {
        if (isStopped) {
            it.onError(MissionStoppedException())
        } else {
            it.onSuccess(1)
        }
    }

    @Synchronized
    fun start() {
        if (maybe == null) {
            throw  MissionNotCreateException()
        }

        if (disposable != null) {
            throw MissionAlreadyStartException()
        }

        if (isStopped) {

        }

        disposable = maybe!!.subscribe()
    }

    @Synchronized
    fun stop() {
        if (maybe == null) {
            throw  MissionNotCreateException()
        }

        if (disposable == null) {
            throw  MissionNotStartException()
        }

        isStopped = true

        dispose(disposable)
    }
}