package zlc.season.rxdownload3.database

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Empty
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.Status

class EmptyDbAdapter : DbAdapter {
    override fun create(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun read(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readStatus(mission: Mission): Status {
        return Empty()
    }

    override fun writeStatus(mission: Mission, status: Status) {
        //Do nothing
    }

    override fun getAllMission(): Maybe<List<Mission>> {
        return Maybe.just(emptyList())
    }
}