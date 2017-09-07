package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import okio.Okio
import retrofit2.Response
import zlc.season.rxdownload3.helper.ResponseUtil.isChunked
import java.io.File
import java.io.File.separator


class NormalTargetFile(val mission: RealMission) {

    private val filePath = mission.actual.savePath + separator + mission.actual.fileName
    val file = File(filePath)

    init {
        val dir = File(mission.actual.savePath)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }

        if (!file.exists()) {
            file.createNewFile()
        } else {
            file.delete()
            file.createNewFile()
        }
    }

    fun save(response: Response<ResponseBody>) {
        val respBody = response.body() ?: throw Throwable("Response body is NULL")

        var downloadSize = 0L
        val byteSize = 8192L
        val status = Downloading(isChunked(response), downloadSize, respBody.contentLength())

        respBody.source().use { source ->
            Okio.buffer(Okio.sink(file)).use { sink ->
                val buffer = sink.buffer()
                var readLen = source.read(buffer, byteSize)

                while (readLen != -1L) {
                    downloadSize += readLen
                    status.downloadSize = downloadSize

                    mission.emitStatus(status)
                    readLen = source.read(buffer, byteSize)
                }
            }
        }
    }
}