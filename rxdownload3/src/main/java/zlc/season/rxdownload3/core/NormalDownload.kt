package zlc.season.rxdownload3.core

import io.reactivex.Maybe
import zlc.season.rxdownload3.http.HttpProcessor


class NormalDownload(missionWrapper: MissionWrapper) : DownloadType(missionWrapper) {

    private val targetFile = NormalTargetFile(missionWrapper)

    override fun download(): Maybe<Any> {
        return HttpProcessor.download(missionWrapper)
                .flatMap {
                    targetFile.save(it)
                    Maybe.just(1)
                }
    }
}