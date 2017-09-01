package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.http.HttpProcessor


class RealMission(val mission: Mission, val processor: FlowableProcessor<DownloadStatus>) {
    var isStoped: Boolean = false

    var isSupportRange: Boolean = false
    var isFileChange: Boolean = false

    var lastModify: Long = 0L

    var contentLength: Long = -1L

    var realFileName = mission.fileName
    var realPath = if (mission.savePath.isEmpty()) DEFAULT_SAVE_PATH else mission.savePath

    var disposable: Disposable? = null
    lateinit var maybe: Maybe<Any>

    init {
        processor.onNext(DownloadStatus(0))
    }


    fun start() {
        maybe = Maybe.just(1)
                .flatMap { HttpProcessor.checkUrl(this) }
                .flatMap { DownloadType.generateType(this) }
                .flatMap { it.download() }
                .doOnError { processor.onError(it) }
                .doOnSuccess { processor.onComplete() }
                .doFinally { MissionBox.remove(this) }

        if (mission.autoStart) {
            disposable = maybe.subscribe()
        }
    }

    fun manualStart() {
        disposable = maybe.subscribe()
    }

    fun stop() {
        dispose(disposable)
    }
}