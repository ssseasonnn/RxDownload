package zlc.season.rxdownload3.database

import zlc.season.rxdownload3.core.DownloadStatus
import zlc.season.rxdownload3.core.Mission


interface DbAdapter {
    fun readStatus(mission: Mission): DownloadStatus

    fun writeStatus(mission: Mission, status: DownloadStatus)

    fun missionFailed(mission: Mission)

    fun missionSuccess(mission: Mission)
}