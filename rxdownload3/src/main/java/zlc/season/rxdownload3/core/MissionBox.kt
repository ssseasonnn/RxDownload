package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.extension.Extension
import java.io.File


interface MissionBox {
    fun isExists(mission: Mission): Maybe<Boolean>

    fun create(mission: Mission): Flowable<Status>

    fun start(mission: Mission): Maybe<Any>

    fun stop(mission: Mission): Maybe<Any>

    fun delete(mission: Mission, deleteFile: Boolean): Maybe<Any>

    fun createAll(missions: List<Mission>): Maybe<Any>

    fun startAll(): Maybe<Any>

    fun stopAll(): Maybe<Any>

    fun deleteAll(deleteFile: Boolean): Maybe<Any>

    fun file(mission: Mission): Maybe<File>

    fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any>

    fun clear(mission: Mission): Maybe<Any>

    fun clearAll(): Maybe<Any>

    fun update(newMission: Mission): Maybe<Any>
}