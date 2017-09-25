package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.extension.Extension
import java.io.File


interface MissionBox {
    fun create(mission: Mission): Flowable<Status>

    fun start(mission: Mission): Maybe<Any>

    fun stop(mission: Mission): Maybe<Any>

    fun startAll(): Maybe<Any>

    fun stopAll(): Maybe<Any>

    fun getFile(mission: Mission): Maybe<File>

    fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any>
}