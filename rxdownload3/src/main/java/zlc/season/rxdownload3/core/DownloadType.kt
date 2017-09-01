package zlc.season.rxdownload3.core

import io.reactivex.Maybe


abstract class DownloadType(val mission: RealMission) {

    abstract fun download(): Maybe<Any>

    companion object {
        fun generateType(mission: RealMission): Maybe<DownloadType> {
            return if (mission.isSupportRange) {
                Maybe.just(RangeDownload(mission))
            } else {
                Maybe.just(NormalDownload(mission))
            }
        }
    }
}