package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.internal.operators.maybe.MaybeToPublisher.INSTANCE
import io.reactivex.schedulers.Schedulers
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.MAX_CONCURRENCY
import zlc.season.rxdownload3.core.RangeTmpFile.Segment
import zlc.season.rxdownload3.helper.logd
import zlc.season.rxdownload3.http.HttpCore


class RangeDownload(mission: RealMission) : DownloadType(mission) {
    private val targetFile = RangeTargetFile(mission)
    private val tmpFile = RangeTmpFile(mission)

    override fun download(): Maybe<Any> {
        if (tmpFile.ensureFinish()) {
            if (targetFile.ensureFinish()) {
                return Maybe.just(ANY)
            } else {
                tmpFile.reset()
            }
        }

        val arrays = mutableListOf<Maybe<Any>>()

        tmpFile.getSegments()
                .filter { !it.isComplete() }
                .forEach { arrays.add(rangeDownload(it)) }


        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE, true, MAX_CONCURRENCY)
                .lastElement()
                .doOnSuccess { targetFile.rename() }
    }


    private fun rangeDownload(segment: Segment): Maybe<Any> {
        return Maybe.just(segment)
                .subscribeOn(Schedulers.io())
                .map {
                    val range = "bytes=${it.start}-${it.end}"
                    logd("Range: $range")
                    return@map range
                }
                .flatMap { HttpCore.download(mission, it) }
                .flatMap {
                    targetFile.save(it, segment, tmpFile)
                    Maybe.just(ANY)
                }
    }
}


