package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe


abstract class DownloadType(val missionWrapper: MissionWrapper) {

    abstract fun download(): Flowable<DownloadStatus>

    companion object {
        fun generateType(missionWrapper: MissionWrapper): Maybe<DownloadType> {
            if (missionWrapper.isSupportRange) {
                return Maybe.just(RangeDownload(missionWrapper))
            } else {
                return Maybe.just(NormalDownload(missionWrapper))
            }
        }
    }
}