package zlc.season.rxdownload3.core

import io.reactivex.Observable
import io.reactivex.processors.BehaviorProcessor
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import io.reactivex.processors.FlowableProcessor as Processor


object MissionBox {
    val QUEUE: BlockingQueue<RealMission> = LinkedBlockingQueue()
    val processorMap = mutableMapOf<String, Processor<DownloadStatus>>()

    fun produce(mission: Mission): Observable<DownloadStatus> {
        var processor = processorMap[mission.tag()]
        if (processor == null) {
            processor = BehaviorProcessor.create<DownloadStatus>().toSerialized()
            processorMap[mission.tag()] = processor
        }

        QUEUE.put(RealMission(mission, processor))
        return processor.toObservable()
    }

    fun consume(): RealMission {
        return QUEUE.take()
    }

}