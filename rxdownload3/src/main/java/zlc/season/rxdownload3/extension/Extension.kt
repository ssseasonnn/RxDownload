package zlc.season.rxdownload3.extension

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.RealMission


interface Extension {
    fun init(mission: RealMission)

    fun action(): Maybe<Any>
}