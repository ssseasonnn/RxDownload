package zlc.season.rxdownload3.database

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission

class EmptyDbActor : DbActor {
    override fun update(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun create(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun read(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun getAllMission(): Maybe<List<Mission>> {
        return Maybe.just(emptyList())
    }
}