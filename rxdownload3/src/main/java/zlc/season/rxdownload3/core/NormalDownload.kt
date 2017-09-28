package zlc.season.rxdownload3.core


import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.http.HttpCore

class NormalDownload(mission: RealMission) : DownloadType(mission) {
    private val targetFile = NormalTargetFile(mission)

    override fun initStatus(withFlag: Boolean) {
        val status = targetFile.getStatus()
        if (withFlag) {
            when {
                targetFile.ensureFinish() -> Succeed(status)
                else -> Suspend(status)
            }
        }
        mission.status = status
    }

    override fun download(): Flowable<Status> {
        if (targetFile.ensureFinish()) {
            return Flowable.empty()
        }

        targetFile.checkFile()

        return Maybe.just(ANY)
                .flatMap { HttpCore.download(mission) }
                .flatMapPublisher {
                    targetFile.save(it)
                }
    }
}