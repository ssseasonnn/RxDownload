package zlc.season.rxdownload3.database

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.RealMission

class EmptyDbActor : DbActor {
    override fun isExists(mission: RealMission): Boolean {
        return false
    }

    override fun update(mission: RealMission) {

    }

    override fun create(mission: RealMission) {

    }

    override fun read(mission: RealMission) {

    }

    override fun delete(mission: RealMission) {

    }


    override fun getAllMission(): Maybe<List<Mission>> {
        return Maybe.just(emptyList())
    }
}