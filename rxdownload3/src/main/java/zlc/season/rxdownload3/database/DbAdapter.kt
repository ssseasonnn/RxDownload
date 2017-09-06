package zlc.season.rxdownload3.database

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.status.Status


interface DbAdapter {
    fun readStatus(mission: Mission): Status

    fun writeStatus(mission: Mission, status: Status)

    fun getAllMission(): Maybe<List<Mission>>
}