package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import okio.Okio
import retrofit2.Response
import zlc.season.rxdownload3.helper.ResponseUtil
import java.io.File


class RangeTargetFile(missionWrapper: MissionWrapper) : DownloadFile(missionWrapper) {
    private val filePath = missionWrapper.realPath + File.separator + missionWrapper.realFileName
    private val file = File(filePath)

     val tmpFile  = RangeTmpFile(missionWrapper)

    fun save(response: Response<ResponseBody>, segment: Segment) {
        val respBody = response.body()
        if (respBody == null) {
            missionWrapper.processor.onError(RuntimeException("body is null"))
            return
        }

        var downloadSize = 0L
        val byteSize = 8192L
        val status = DownloadStatus(isChunked = ResponseUtil.isChunked(response), totalSize = respBody.contentLength())

        respBody.source().use(missionWrapper.processor) { source ->
            Okio.buffer(Okio.sink(file)).use(missionWrapper.processor) { sink ->
                val buffer = sink.buffer()
                var readLen = source.read(buffer, byteSize)
                while (readLen != -1L) {
                    downloadSize += readLen
                    status.downloadSize = downloadSize
                    missionWrapper.processor.onNext(status)
                    readLen = source.read(buffer, byteSize)
                }
            }
        }

    }
}


