package zlc.season.rxdownload3.core

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.http.HttpProcessor


class NormalDownload(val missionWrapper: MissionWrapper) : DownloadType {

    override fun download(): Flowable<DownloadStatus> {
        return HttpProcessor.download(missionWrapper)
                .flatMap { resp ->

                    Flowable.just(DownloadStatus(1))
                }
    }

    fun saveFile(resp: Response<ResponseBody>): Flowable<DownloadStatus> {
        return Flowable.create({ emitter ->

        }, BackpressureStrategy.LATEST)
    }

    fun realSave(emitter: FlowableEmitter<DownloadStatus>, resp: Response<ResponseBody>) {

    }
}