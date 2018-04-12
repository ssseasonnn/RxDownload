package zlc.season.rxdownload3

import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.Mission
import zlc.season.rxdownload3.core.Status
import zlc.season.rxdownload3.extension.Extension
import java.io.File


interface RxDownloadI {
    fun isExists(url: String): Maybe<Boolean>

    fun isExists(mission: Mission): Maybe<Boolean>

    fun create(url: String): Flowable<Status>

    fun create(mission: Mission): Flowable<Status>

    fun update(newMission: Mission): Maybe<Any>

    fun start(url: String): Maybe<Any>

    fun start(mission: Mission): Maybe<Any>

    fun stop(url: String): Maybe<Any>

    fun stop(mission: Mission): Maybe<Any>

    fun delete(url: String, deleteFile: Boolean = false): Maybe<Any>

    fun delete(mission: Mission, deleteFile: Boolean = false): Maybe<Any>

    fun clear(url: String): Maybe<Any>

    fun clear(mission: Mission): Maybe<Any>

    fun getAllMission(): Maybe<List<Mission>>

    fun createAll(missions: List<Mission>): Maybe<Any>

    fun startAll(): Maybe<Any>

    fun stopAll(): Maybe<Any>

    fun deleteAll(deleteFile: Boolean = false): Maybe<Any>

    fun clearAll(): Maybe<Any>

    fun file(url: String): Maybe<File>

    fun file(mission: Mission): Maybe<File>

    fun extension(url: String, type: Class<out Extension>): Maybe<Any>

    fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any>
}
