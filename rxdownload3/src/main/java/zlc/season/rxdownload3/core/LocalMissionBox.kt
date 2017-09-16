package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.internal.operators.maybe.MaybeToPublisher.INSTANCE
import zlc.season.rxdownload3.core.DownloadConfig.maxMission
import java.util.concurrent.Semaphore


class LocalMissionBox : MissionBox {
    private val semaphore = Semaphore(maxMission, true)

    private val SET = mutableSetOf<RealMission>()

    override fun create(mission: Mission): Flowable<Status> {
        val realMission = SET.find { it.actual == mission }

        return if (realMission != null) {
            realMission.getProcessor()
        } else {
            val new = RealMission(semaphore, mission)
            SET.add(new)
            new.getProcessor()
        }
    }

    override fun start(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.empty()

        return realMission.start()
    }

    override fun stop(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.empty()

        return realMission.stop()
    }

    override fun startAll(): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.start()) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE, true, maxMission)
                .lastElement()
    }


    override fun stopAll(): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.stop()) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE)
                .lastElement()
    }
}