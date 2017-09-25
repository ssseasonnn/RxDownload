package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.internal.operators.maybe.MaybeToPublisher.INSTANCE
import io.reactivex.schedulers.Schedulers
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.core.DownloadConfig.maxRange
import zlc.season.rxdownload3.core.RangeTmpFile.Segment
import zlc.season.rxdownload3.helper.logd
import zlc.season.rxdownload3.http.HttpCore


class RangeDownload(mission: RealMission) : DownloadType(mission) {
    private val targetFile = RangeTargetFile(mission)
    private val tmpFile = RangeTmpFile(mission)

    override fun initStatus(withFlag: Boolean) {
        val status = tmpFile.currentStatus()

        if (withFlag) {
            when {
                isFinish() -> status.toSucceed()
                else -> status.toSuspend()
            }
        }

        mission.setStatus(status)
    }

    private fun isFinish(): Boolean {
        return tmpFile.isFinish() && targetFile.isFinish()
    }

    override fun download(): Maybe<Any> {
        if (isFinish()) {

            return Maybe.just(ANY)
        }

        val arrays = mutableListOf<Maybe<Any>>()

        if (targetFile.isShadowExists()) {
            tmpFile.checkFile()
        } else {
            tmpFile.reset()
        }

        tmpFile.getSegments()
                .filter { !it.isComplete() }
                .forEach { arrays.add(rangeDownload(it)) }

        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE, true, maxRange)
                .lastElement()
                .doOnSuccess { targetFile.rename() }
    }


    private fun rangeDownload(segment: Segment): Maybe<Any> {
        return Maybe.just(segment)
                .subscribeOn(Schedulers.io())
                .map { "bytes=${it.current}-${it.end}" }
                .doOnSuccess { logd("Range: $it") }
                .flatMap { HttpCore.download(mission, it) }
                .flatMap { targetFile.save(it, segment, tmpFile) }
    }
}


