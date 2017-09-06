package zlc.season.rxdownload3.database

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.status.Status
import zlc.season.rxdownload3.status.Waiting


class SqliteAdapter : DbAdapter {
    init {

    }

    override fun getAllMission(): Maybe<List<Mission>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writeStatus(mission: Mission, status: Status) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readStatus(mission: Mission): Status {
        return Waiting()
    }
}