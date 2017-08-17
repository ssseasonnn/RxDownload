package zlc.season.rxdownload3.core

import io.reactivex.processors.FlowableProcessor


class MissionWrapper(val mission: Mission, val processor: FlowableProcessor<DownloadStatus>) {


    val rangeSupport: Boolean = false
    val fileChanged: Boolean = false

    val lastModify: String = ""

    val threads: Int = 3
    val perSize: Int = 5 * 1024  //KB

    init {
        processor.onNext(DownloadStatus(0))
    }

    fun start() {

    }


}