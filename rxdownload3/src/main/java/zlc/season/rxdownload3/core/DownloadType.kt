package zlc.season.rxdownload3.core

import io.reactivex.Flowable


abstract class DownloadType(val mission: RealMission) {
    abstract fun initStatus(withFlag: Boolean)

    abstract fun download(): Flowable<Status>
}