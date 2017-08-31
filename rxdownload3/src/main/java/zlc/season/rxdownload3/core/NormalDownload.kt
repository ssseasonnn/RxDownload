package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import zlc.season.rxdownload3.http.HttpProcessor


class NormalDownload(mission: RealMission) : DownloadType(mission) {

    private val targetFile = NormalTargetFile(mission)

    override fun download(): Maybe<Any> {
        return HttpProcessor.download(realMission)
                .flatMap {
                    targetFile.save(it)
                    Maybe.just(1)
                }
    }
}