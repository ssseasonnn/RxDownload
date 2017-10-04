package zlc.season.rxdownload3.database

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.RealMission


interface DbActor {
    fun isExists(mission: RealMission): Boolean

    fun create(mission: RealMission)

    fun read(mission: RealMission)

    fun update(mission: RealMission)

    fun delete(mission: RealMission)

    fun getAllMission(): Maybe<List<Mission>>
}