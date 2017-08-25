package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.helper.ResponseUtil.Companion.isChunked
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class NormalTargetFile(missionWrapper: MissionWrapper) : DownloadFile(missionWrapper) {

    val filePath = missionWrapper.realPath + File.separator + missionWrapper.realFileName
    val file = File(filePath)

    init {
        val dir = File(missionWrapper.realPath)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }

        if (!file.exists()) {
            file.createNewFile()
        }
    }

    fun save(response: Response<ResponseBody>) {
        var downloadSize = 0L
        val buffer = ByteArray(8192)

        val respBody = response.body()
        if (respBody == null) {
            missionWrapper.processor.onError(RuntimeException("body is null"))
            return
        }

//        val source = Okio.buffer(Okio.source(respBody.byteStream()))
//        val sink = Okio.buffer(Okio.sink(file))

        val source = respBody.byteStream()
        val sink = FileOutputStream(file)

        val status = DownloadStatus(isChunked = isChunked(response), totalSize = respBody.contentLength())


        try {
            var readLen = source.read(buffer)
            while (readLen != -1) {
                sink.write(buffer, 0, buffer.size)
                downloadSize += readLen
                status.downloadSize = downloadSize
                missionWrapper.processor.onNext(status)

                readLen = source.read(buffer)
            }

            sink.flush()
        } catch (e: IOException) {
            missionWrapper.processor.onError(e)
        } finally {
            sink.close()
            source.close()
        }
    }
}