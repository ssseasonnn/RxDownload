package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.internal.operators.maybe.MaybeToPublisher.INSTANCE
import zlc.season.rxdownload3.core.DownloadConfig.MAX_CONCURRENCY
import zlc.season.rxdownload3.core.RangeTmpFile.Segment
import zlc.season.rxdownload3.http.HttpProcessor


class RangeDownload(mission: RealMission) : DownloadType(mission) {
    private val targetFile = RangeTargetFile(mission)
    private val tmpFile = RangeTmpFile(mission)

    override fun download(): Maybe<Any> {
        if (tmpFile.isFinish()) {
            return Maybe.just(1)
        }

        val arrays = mutableListOf<Maybe<Any>>()

        tmpFile.getSegments()
                .filter { !it.isComplete() }
                .forEach { arrays.add(rangeDownload(it)) }


        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE, true, MAX_CONCURRENCY)
                .lastElement()
    }


    private fun rangeDownload(segment: Segment): Maybe<Any> {
        return Maybe.just(segment)
                .map { "bytes=${it.start}-${it.end}" }
                .flatMap { HttpProcessor.download(mission, it) }
                .flatMap {
                    targetFile.save(it, segment)
                    Maybe.just(1)
                }
    }
}


