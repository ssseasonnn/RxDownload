package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import zlc.season.rxdownload3.http.HttpCore


class NormalDownload(mission: RealMission) : DownloadType(mission) {

    private val targetFile = NormalTargetFile(mission)

    override fun download(): Maybe<Any> {
        return Maybe.just(1)
                .flatMap { HttpCore.download(mission) }
                .flatMap {
                    targetFile.save(it)
                    Maybe.just(1)
                }
    }
}