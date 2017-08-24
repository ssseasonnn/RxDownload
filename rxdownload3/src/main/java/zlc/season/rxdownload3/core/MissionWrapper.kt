package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.processors.FlowableProcessor
import zlc.season.rxdownload3.core.DownloadConfig.defaultSavePath
import zlc.season.rxdownload3.http.HttpProcessor


class MissionWrapper(val mission: Mission, val processor: FlowableProcessor<DownloadStatus>) {

    var isSupportRange: Boolean = false
    var isFileChange: Boolean = false

    var lastModify: Long = 0L

    var realFileName = mission.fileName()
    var realPath = if (mission.savePath().isEmpty()) defaultSavePath else mission.savePath()

    init {
        processor.onNext(DownloadStatus(0))
    }


    fun start() {
        Maybe.just(1)
                .flatMap { HttpProcessor.checkUrl(this) }
                .flatMap { DownloadType.generateType(this) }
                .doOnSuccess { type -> type.download() }
                .doOnError {
                    println(it)
                }
                .subscribe()
    }


}