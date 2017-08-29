package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.http.HttpProcessor


class RangeDownload(missionWrapper: MissionWrapper) : DownloadType(missionWrapper) {
    private val targetFile = RangeTargetFile(missionWrapper)

    override fun download(): Maybe<Any> {
        if (targetFile.tmpFile.isFinish()) {
            return Maybe.just(1)
        }

        val segments = targetFile.tmpFile.read().filter {
            it.isComplete()
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
                .flatMap { HttpProcessor.download(missionWrapper, it) }
                .flatMap {
                    targetFile.save(it, segment)
                    Maybe.just(1)
                }
    }
}


