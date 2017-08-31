package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.RangeTmpFile.FileSegment.Segment
import zlc.season.rxdownload3.http.HttpProcessor


class RangeDownload(mission: RealMission) : DownloadType(mission) {
    private val targetFile = RangeTargetFile(mission)

    override fun download(): Maybe<Any> {
        if (targetFile.tmpFile.fileHeader.isFinish()) {
            return Maybe.just(1)
        }

        val segments = targetFile.tmpFile.fileSegment.segments.filter {
            !it.isComplete()
        }

        val arrays = mutableListOf<Maybe<Any>>()

        segments.forEach {
            arrays.add(rangeDownload(it))
        }

        return Maybe.merge(Flowable.fromIterable(arrays), DownloadConfig.MAX_CONCURRENCY).lastElement()
    }


    private fun rangeDownload(segment: Segment): Maybe<Any> {
        return Maybe.just(segment)
                .map { "bytes=${it.start}-${it.end}" }
                .flatMap { HttpProcessor.download(realMission, it) }
                .flatMap {
                    targetFile.save(it, segment)
                    Maybe.just(1)
                }
    }
}


