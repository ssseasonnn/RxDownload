package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.internal.operators.maybe.MaybeToPublisher.INSTANCE
import io.reactivex.schedulers.Schedulers
import zlc.season.rxdownload3.core.DownloadConfig.MAX_CONCURRENCY
import zlc.season.rxdownload3.core.RangeTmpFile.Segment
import zlc.season.rxdownload3.http.HttpCore


class RangeDownload(mission: RealMission) : DownloadType(mission) {
    private val targetFile = RangeTargetFile(mission)
    private val tmpFile = RangeTmpFile(mission)

    override fun download(): Maybe<Any> {
        if (tmpFile.isFinish()) {
            throw  RuntimeException("Mission already finished")
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
                .subscribeOn(Schedulers.io())
                .map { "bytes=${it.start}-${it.end}" }
                .flatMap { HttpCore.download(mission, it) }
                .flatMap {
                    targetFile.save(it, segment, tmpFile)
                    Maybe.just(1)
                }
    }
}


