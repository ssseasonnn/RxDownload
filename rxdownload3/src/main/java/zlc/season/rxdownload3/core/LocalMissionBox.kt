package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.internal.operators.maybe.MaybeToPublisher.INSTANCE
import io.reactivex.schedulers.Schedulers.*
import zlc.season.rxdownload3.extension.Extension
import zlc.season.rxdownload3.helper.ANY
import java.io.File
import java.util.concurrent.Semaphore

class LocalMissionBox : MissionBox {
    private val maxMission = DownloadConfig.maxMission
    private val semaphore = Semaphore(maxMission, true)

    private val SET = mutableSetOf<RealMission>()

    override fun isExists(mission: Mission): Maybe<Boolean> {
        return Maybe.create<Boolean> {
            val result = SET.find { it.actual == mission }
            if (result != null) {
                it.onSuccess(true)
            } else {
                val tmpMission = RealMission(mission, semaphore, false)
                if (DownloadConfig.enableDb) {
                    val isExists = DownloadConfig.dbActor.isExists(tmpMission)
                    it.onSuccess(isExists)
                } else {
                    it.onSuccess(false)
                }
            }
        }
    }

    override fun create(mission: Mission): Flowable<Status> {
        val realMission = SET.find { it.actual == mission }

        return if (realMission != null) {
            realMission.getFlowable()
        } else {
            val new = RealMission(mission, semaphore)
            SET.add(new)
            new.getFlowable()
        }
    }

    override fun update(newMission: Mission): Maybe<Any> {
        return Maybe.create<Any> {
            val tmpMission = RealMission(newMission, semaphore, false)
            if (DownloadConfig.enableDb) {
                if (DownloadConfig.dbActor.isExists(tmpMission)) {
                    DownloadConfig.dbActor.update(tmpMission)
                }
            }
            it.onSuccess(ANY)
        }
    }


    override fun start(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))

        return realMission.start()
    }

    override fun stop(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))

        return realMission.stop()
    }

    override fun delete(mission: Mission, deleteFile: Boolean): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))
        return realMission.delete(deleteFile)
    }

    override fun createAll(missions: List<Mission>): Maybe<Any> {
        return Maybe.create<Any> {
            missions.forEach { mission ->
                val realMission = SET.find { it.actual == mission }
                if (realMission == null) {
                    val new = RealMission(mission, semaphore)
                    SET.add(new)
                }
            }
            it.onSuccess(ANY)
        }.subscribeOn(newThread())
    }

    override fun startAll(): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.start()) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE, true)
                .lastElement()
    }

    override fun stopAll(): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.stop()) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE)
                .lastElement()
    }

    override fun deleteAll(deleteFile: Boolean): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.delete(deleteFile)) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE)
                .lastElement()
    }

    override fun file(mission: Mission): Maybe<File> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))
        return realMission.file()
    }

    override fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))

        return realMission.findExtension(type).action()
    }

    override fun clear(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))

        return Maybe.create<Any> {
            //stop first.
            realMission.realStop()
            SET.remove(realMission)
            it.onSuccess(ANY)
        }
    }

    override fun clearAll(): Maybe<Any> {
        return Maybe.create<Any> {
            SET.forEach { it.realStop() }
            SET.clear()
        }
    }
}
