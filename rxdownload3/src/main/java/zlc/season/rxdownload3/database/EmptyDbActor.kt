package zlc.season.rxdownload3.database

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission

class EmptyDbActor : DbActor {
    override fun isExists(mission: Mission): Boolean {
        return false
    }

    override fun update(mission: Mission) {

    }

    override fun create(mission: Mission) {

    }

    override fun read(mission: Mission) {

    }

    override fun delete(mission: Mission) {

    }


    override fun getAllMission(): Maybe<List<Mission>> {
        return Maybe.just(emptyList())
    }
}