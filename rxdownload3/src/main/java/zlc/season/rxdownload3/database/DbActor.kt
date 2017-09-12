package zlc.season.rxdownload3.database

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission


interface DbActor {
    fun isExists(mission: Mission): Boolean

    fun create(mission: Mission)

    fun read(mission: Mission)

    fun update(mission: Mission)

    fun delete(mission: Mission)

    fun getAllMission(): Maybe<List<Mission>>
}