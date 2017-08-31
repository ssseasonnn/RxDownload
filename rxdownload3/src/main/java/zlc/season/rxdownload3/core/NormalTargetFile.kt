package zlc.season.rxdownload3.core

import io.reactivex.processors.FlowableProcessor
import okhttp3.ResponseBody
import okio.Okio
import retrofit2.Response
import zlc.season.rxdownload3.helper.ResponseUtil.Companion.isChunked
import java.io.Closeable
import java.io.File


class NormalTargetFile(mission: RealMission) : DownloadFile(mission) {

    private val filePath = mission.realPath + File.separator + mission.realFileName
    val file = File(filePath)

    init {
        val dir = File(mission.realPath)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }

        if (!file.exists()) {
            file.createNewFile()
        }
    }

    fun save(response: Response<ResponseBody>) {
        val respBody = response.body()
        if (respBody == null) {
            mission.processor.onError(RuntimeException("body is null"))
            return
        }

        var downloadSize = 0L
        val byteSize = 8192L
        val status = DownloadStatus(isChunked = isChunked(response), totalSize = respBody.contentLength())

        respBody.source().use(mission.processor) { source ->
            Okio.buffer(Okio.sink(file)).use(mission.processor) { sink ->
                val buffer = sink.buffer()
                var readLen = source.read(buffer, byteSize)
                while (readLen != -1L) {
                    downloadSize += readLen
                    status.downloadSize = downloadSize
                    mission.processor.onNext(status)
                    readLen = source.read(buffer, byteSize)
                }
            }
        }
    }
}

inline fun <T : Closeable?, R> T.use(processor: FlowableProcessor<DownloadStatus>, block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        processor.onError(e)
        try {
            this?.close()
        } catch (closeException: Exception) {
            processor.onError(e)
        }
        throw e
    } finally {
        if (!closed) {
            this?.close()
        }
        processor.onComplete()
    }
}