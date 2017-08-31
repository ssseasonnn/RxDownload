package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.processors.FlowableProcessor
import zlc.season.rxdownload3.core.DownloadConfig.DEFAULT_SAVE_PATH
import zlc.season.rxdownload3.helper.Logger
import zlc.season.rxdownload3.http.HttpProcessor


class RealMission(val mission: Mission, val processor: FlowableProcessor<DownloadStatus>) {

    var isSupportRange: Boolean = false
    var isFileChange: Boolean = false

    var lastModify: Long = 0L

    var contentLength: Long = -1L

    var realFileName = mission.fileName()
    var realPath = if (mission.savePath().isEmpty()) DEFAULT_SAVE_PATH else mission.savePath()

    init {
        processor.onNext(DownloadStatus(0))
    }


    fun start() {
        Maybe.just(1)
                .flatMap { HttpProcessor.checkUrl(this) }
                .flatMap { DownloadType.generateType(this) }
                .flatMap { it.download() }
                .doOnError { Logger.loge("Mission Failed", it) }
                .subscribe()
    }


}