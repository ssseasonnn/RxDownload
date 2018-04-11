package zlc.season.rxdownload3

import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.DownloadCore
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.Status
import zlc.season.rxdownload3.extension.Extension
import java.io.File


object RxDownload : RxDownloadI {
    private val downloadCore = DownloadCore()

    override fun isExists(url: String): Maybe<Boolean> {
        return isExists(Mission(url))
    }

    override fun isExists(mission: Mission): Maybe<Boolean> {
        return downloadCore.isExists(mission)
    }

    override fun create(url: String): Flowable<Status> {
        return create(Mission(url))
    }

    override fun create(mission: Mission): Flowable<Status> {
        return downloadCore.create(mission)
    }

    override fun update(newMission: Mission): Maybe<Any> {
        return downloadCore.update(newMission)
    }

    override fun start(url: String): Maybe<Any> {
        return start(Mission(url))
    }

    override fun start(mission: Mission): Maybe<Any> {
        return downloadCore.start(mission)
    }

    override fun stop(url: String): Maybe<Any> {
        return stop(Mission(url))
    }

    override fun stop(mission: Mission): Maybe<Any> {
        return downloadCore.stop(mission)
    }

    override fun delete(url: String, deleteFile: Boolean): Maybe<Any> {
        return delete(Mission(url), deleteFile)
    }

    override fun delete(mission: Mission, deleteFile: Boolean): Maybe<Any> {
        return downloadCore.delete(mission, deleteFile)
    }

    override fun clear(url: String): Maybe<Any> {
        return clear(Mission(url))
    }

    override fun clear(mission: Mission): Maybe<Any> {
        return downloadCore.clear(mission)
    }

    override fun getAllMission(): Maybe<List<Mission>> {
        return downloadCore.getAllMission()
    }

    override fun createAll(missions: List<Mission>): Maybe<Any> {
        return downloadCore.createAll(missions)
    }

    override fun startAll(): Maybe<Any> {
        return downloadCore.startAll()
    }

    override fun stopAll(): Maybe<Any> {
        return downloadCore.stopAll()
    }

    override fun deleteAll(deleteFile: Boolean): Maybe<Any> {
        return downloadCore.deleteAll(deleteFile)
    }

    override fun clearAll(): Maybe<Any> {
        return downloadCore.clearAll()
    }

    override fun file(url: String): Maybe<File> {
        return file(Mission(url))
    }

    override fun file(mission: Mission): Maybe<File> {
        return downloadCore.file(mission)
    }

    override fun extension(url: String, type: Class<out Extension>): Maybe<Any> {
        return extension(Mission(url), type)
    }

    override fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any> {
        return downloadCore.extension(mission, type)
    }
}