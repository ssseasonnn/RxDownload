package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import java.util.concurrent.LinkedBlockingQueue
import io.reactivex.processors.FlowableProcessor as Processor


object MissionBox {
    val SET = mutableSetOf<RealMission>()
    val QUEUE = LinkedBlockingQueue<RealMission>()

    fun produce(mission: Mission): Flowable<DownloadStatus> {
        val processor = BehaviorProcessor.create<DownloadStatus>().toSerialized()
        if (isMissionExists(mission)) {
            processor.onError(MissionExitsException())
        } else {
            val realMission = RealMission(mission, processor)
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

    fun start(mission: Mission) {
        val realMission: RealMission = SET.find { it.mission.tag == mission.tag } ?: return
        realMission.manualStart()
    }

    fun stop(mission: Mission) {
        val realMission: RealMission = SET.find { it.mission.tag == mission.tag } ?: return
        realMission.isStoped = true

        realMission.processor.onError(MissionStoppedException())
        SET.remove(realMission)
    }

    private fun isMissionExists(mission: Mission): Boolean {
        return SET.any { it.mission.tag == mission.tag }
    }
}