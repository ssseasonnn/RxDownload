package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import okio.Okio
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.FACTORY
import zlc.season.rxdownload3.helper.ResponseUtil.Companion.isChunked
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
            mission.processor.onNext(FACTORY.failed(RuntimeException("body is null")))
            return
        }

        var downloadSize = 0L
        val byteSize = 8192L

        respBody.source().use { source ->
            Okio.buffer(Okio.sink(file)).use { sink ->
                val buffer = sink.buffer()
                var readLen = source.read(buffer, byteSize)
                while (readLen != -1L) {
                    downloadSize += readLen
                    val status = FACTORY.downloading(isChunked(response), downloadSize, respBody.contentLength())
                    mission.processor.onNext(status)
                    readLen = source.read(buffer, byteSize)
                }
            }
        }
    }
}