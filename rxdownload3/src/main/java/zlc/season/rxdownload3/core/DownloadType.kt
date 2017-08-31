package zlc.season.rxdownload3.core

import io.reactivex.Maybe


abstract class DownloadType(val realMission: RealMission) {

    abstract fun download(): Maybe<Any>

    companion object {
        fun generateType(realMission: RealMission): Maybe<DownloadType> {
            return if (realMission.isSupportRange) {
                Maybe.just(RangeDownload(realMission))
            } else {
                Maybe.just(NormalDownload(realMission))
            }
        }
    }
}