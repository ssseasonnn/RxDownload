package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.helper.ResponseUtil.Companion.isChunked
import zlc.season.rxdownload3.helper.using
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


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
        using {
            var downloadSize = 0L
            val buffer = kotlin.ByteArray(8)

            val respBody = response.body()
            if (respBody == null) {
                missionWrapper.processor.onError(RuntimeException("body is null"))
                return@using
            }

            val fileInStream: InputStream = respBody.byteStream().autoClose()
            val fileOutSteam: OutputStream = FileOutputStream(file).autoClose()

            val status = DownloadStatus(isChunked = isChunked(response), totalSize = respBody.contentLength())

            var readLen = fileInStream.read(buffer)
            while (readLen != -1) {
                fileOutSteam.write(buffer, 0, readLen)
                downloadSize += readLen
                status.downloadSize = downloadSize
                missionWrapper.processor.onNext(status)

                readLen = fileInStream.read(buffer)
            }

            missionWrapper.processor.onComplete()
        }
    }
}