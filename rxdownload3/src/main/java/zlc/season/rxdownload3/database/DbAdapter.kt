package zlc.season.rxdownload3.database

import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.status.Status


interface DbAdapter<T : Status> {
    fun readStatus(mission: Mission): T

    fun writeStatus(mission: Mission, status: T)

    fun missionFailed(mission: Mission)

    fun missionSuccess(mission: Mission)
}