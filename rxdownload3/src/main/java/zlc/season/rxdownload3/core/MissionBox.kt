package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.processors.BehaviorProcessor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import io.reactivex.processors.FlowableProcessor as Processor


object MissionBox {
    val semaphore = Semaphore(DownloadConfig.MAX_MISSION_NUMBER)

    val SET = mutableSetOf<RealMission>()
    val QUEUE = LinkedBlockingQueue<RealMission>()

    fun produce(mission: Mission): Flowable<DownloadStatus> {
        val processor = BehaviorProcessor.create<DownloadStatus>().toSerialized()
        if (isMissionExists(mission)) {
            processor.onError(MissionExitsException())
        } else {
            val realMission = RealMission(semaphore, mission, processor)
            QUEUE.put(realMission)
            SET.add(realMission)
        }
        return processor
    }

    fun consume(): RealMission {
        return QUEUE.take()
    }

    fun remove(mission: RealMission) {
        SET.remove(mission)
    }

    fun start(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.mission.tag == mission.tag }
                ?: return Maybe.error(MissionNotExistsException())

        return Maybe.create {
            semaphore.acquire()
            realMission.start()
            it.onSuccess(Any())
        }
    }

    fun stop(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.mission.tag == mission.tag } ?:
                return Maybe.error(MissionNotExistsException())

        return Maybe.create {
            realMission.stop()
            it.onSuccess(Any())
        }
    }

    private fun isMissionExists(mission: Mission): Boolean {
        return SET.any { it.mission.tag == mission.tag }
    }
}