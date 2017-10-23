package zlc.season.rxdownload3

import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.DownloadCore
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.Status
import zlc.season.rxdownload3.extension.Extension
import java.io.File


object RxDownload {
    private val downloadCore = DownloadCore()

    fun create(url: String): Flowable<Status> {
        return create(Mission(url))
    }

    fun create(mission: Mission): Flowable<Status> {
        return downloadCore.create(mission)
    }

    fun start(url: String): Maybe<Any> {
        return start(Mission(url))
    }

    fun start(mission: Mission): Maybe<Any> {
        return downloadCore.start(mission)
    }

    fun stop(url: String): Maybe<Any> {
        return stop(Mission(url))
    }

    fun stop(mission: Mission): Maybe<Any> {
        return downloadCore.stop(mission)
    }

    fun delete(url: String, deleteFile: Boolean = false): Maybe<Any> {
        return delete(Mission(url), deleteFile)
    }

    fun delete(mission: Mission, deleteFile: Boolean = false): Maybe<Any> {
        return downloadCore.delete(mission, deleteFile)
    }

    fun getAllMission(): Maybe<List<Mission>> {
        return downloadCore.getAllMission()
    }

    fun createAll(missions: List<Mission>): Maybe<Any> {
        return downloadCore.createAll(missions)
    }

    fun startAll(): Maybe<Any> {
        return downloadCore.startAll()
    }

    fun stopAll(): Maybe<Any> {
        return downloadCore.stopAll()
    }

    fun deleteAll(deleteFile: Boolean = false): Maybe<Any> {
        return downloadCore.deleteAll(deleteFile)
    }

    fun file(url: String): Maybe<File> {
        return file(Mission(url))
    }

    fun file(mission: Mission): Maybe<File> {
        return downloadCore.file(mission)
    }

    fun extension(url: String, type: Class<out Extension>): Maybe<Any> {
        return extension(Mission(url), type)
    }

    fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any> {
        return downloadCore.extension(mission, type)
    }
}