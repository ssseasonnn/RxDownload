package zlc.season.rxdownload3.database

import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.status.DownloadStatus


class SqliteAdapter : DbAdapter<DownloadStatus> {
    init {

    }

    override fun missionFailed(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun missionSuccess(mission: Mission) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writeStatus(mission: Mission, status: DownloadStatus) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readStatus(mission: Mission): DownloadStatus {
        return DownloadStatus()
    }
}