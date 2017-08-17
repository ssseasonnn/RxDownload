package zlc.season.rxdownload3.core

import io.reactivex.Observable
import io.reactivex.processors.BehaviorProcessor
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import io.reactivex.processors.FlowableProcessor as Processor


object MissionBox {
    val queue: BlockingQueue<MissionWrapper> = LinkedBlockingQueue()
    val processorMap = mutableMapOf<String, Processor<DownloadStatus>>()

    fun produce(mission: Mission): Observable<DownloadStatus> {
        var processor = processorMap[mission.provideTag()]
        if (processor == null) {
            processor = BehaviorProcessor.create<DownloadStatus>().toSerialized()
            processorMap[mission.provideTag()] = processor
        }

        queue.put(MissionWrapper(mission, processor))
        return processor.toObservable()
    }

    fun consume(): MissionWrapper {
        return queue.take()
    }

}