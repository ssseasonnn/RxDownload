package zlc.season.rxdownload3.core


import io.reactivex.Maybe
import zlc.season.rxdownload3.core.DownloadConfig.ANY
import zlc.season.rxdownload3.http.HttpCore

class NormalDownload(mission: RealMission) : DownloadType(mission) {
    private val targetFile = NormalTargetFile(mission)

    override fun initStatus() {
        mission.emitStatus(targetFile.getStatus())
    }

    override fun download(): Maybe<Any> {
        if (targetFile.ensureFinish()) {
            return Maybe.just(ANY)
        }

        return Maybe.just(ANY)
                .flatMap { HttpCore.download(mission) }
                .flatMap {
                    targetFile.save(it)
                    Maybe.just(ANY)
                }
    }
}