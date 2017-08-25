package zlc.season.rxdownload3.core

import io.reactivex.Maybe


abstract class DownloadType(val missionWrapper: MissionWrapper) {

    abstract fun download(): Maybe<Any>

    companion object {
        fun generateType(missionWrapper: MissionWrapper): Maybe<DownloadType> {
            return if (missionWrapper.isSupportRange) {
                Maybe.just(RangeDownload(missionWrapper))
            } else {
                Maybe.just(NormalDownload(missionWrapper))
            }
        }
    }
}