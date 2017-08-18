package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import io.reactivex.processors.FlowableProcessor
import zlc.season.rxdownload3.http.HttpProcessor


class MissionWrapper(val mission: Mission, val processor: FlowableProcessor<DownloadStatus>) {

    var isSupportRange: Boolean = false
    var isFileChange: Boolean = false

    var lastModify: Long = 0L

    lateinit var realFileName: String

    lateinit var downloadType: DownloadType

    lateinit var tmpFile: TmpFile
    lateinit var targetFile: TargetFile

    init {


        processor.onNext(DownloadStatus(0))
    }

    fun createDownloadType() {
        if (isSupportRange) {
            downloadType = NormalDownload(this)
        } else {
            downloadType = RangeDownload()
        }
    }

    fun start() {
        Maybe.just(1)
                .flatMap { HttpProcessor.checkUrl(this) }
                .doOnSuccess { createDownloadType() }
                .toFlowable()
                .flatMap { downloadType.download() }
                .subscribe({ println("next") }, { println("error") }, { println("complete") })
    }


}