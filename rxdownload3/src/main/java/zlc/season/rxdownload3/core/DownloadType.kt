package zlc.season.rxdownload3.core

import io.reactivex.Maybe


abstract class DownloadType(val mission: RealMission) {
    abstract fun prepare()

    abstract fun download(): Maybe<Any>
}