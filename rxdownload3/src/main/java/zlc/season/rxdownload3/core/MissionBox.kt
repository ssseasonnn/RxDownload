package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.internal.operators.maybe.MaybeToPublisher.INSTANCE
import io.reactivex.processors.BehaviorProcessor
import zlc.season.rxdownload3.core.DownloadConfig.MAX_MISSION_NUMBER
import java.util.concurrent.Semaphore


object MissionBox {
    private val semaphore = Semaphore(MAX_MISSION_NUMBER, true)

    private val SET = mutableSetOf<RealMission>()

    fun create(mission: Mission): Flowable<DownloadStatus> {
        val realMission = SET.find { it.mission == mission }
        if (realMission != null) {
            return realMission.processor.onBackpressureLatest()
        } else {
            val processor = BehaviorProcessor.create<DownloadStatus>().toSerialized()
            val tmp = RealMission(semaphore, mission, processor)
            SET.add(tmp)
            return processor.onBackpressureLatest()
        }
    }

    fun remove(mission: RealMission) {
        SET.remove(mission)
    }

    fun start(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.mission == mission } ?:
                return Maybe.error(RuntimeException("Mission not exists"))

        return realMission.start()
    }

    fun stop(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.mission == mission } ?:
                return Maybe.error(RuntimeException("Mission not exists"))

        return realMission.stop()
    }

    fun startAll(): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.start()) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE, true, MAX_MISSION_NUMBER)
                .lastElement()
    }


    fun stopAll(): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.stop()) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE, true, MAX_MISSION_NUMBER)
                .lastElement()
    }
}