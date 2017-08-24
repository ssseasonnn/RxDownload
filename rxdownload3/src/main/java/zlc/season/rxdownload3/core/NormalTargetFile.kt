package zlc.season.rxdownload3.core

import io.reactivex.FlowableEmitter
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.helper.ResponseUtil
import zlc.season.rxdownload3.helper.ResponseUtil.Companion.isChunked
import zlc.season.rxdownload3.helper.using
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


class NormalTargetFile(path: String, saveName: String) : DownloadFile(path, saveName) {

    val file: File = File(path + File.separator + saveName)

    fun save(emitter: FlowableEmitter<DownloadStatus>, response: Response<ResponseBody>) {
        var downloadSize = 0L

        val fileInStream: InputStream
        val fileOutSteam: OutputStream

        val buffer: ByteArray = kotlin.ByteArray(8)

        val respBody = response.body()
        if (respBody == null) {
            emitter.onError(RuntimeException("body is null"))
            return
        }

        fileInStream = respBody.byteStream()
        fileOutSteam = FileOutputStream(file)

        val isChuncked = isChunked(response)
        val contentLength = respBody.contentLength()

        val status = DownloadStatus()
        status.isChunked = isChuncked
        status.totalSize = contentLength

        var readLen = fileInStream.read(buffer)
        while (readLen != -1 && !emitter.isCancelled) {
            fileOutSteam.write(buffer, 0, readLen)
            downloadSize += readLen
            status.downloadSize = downloadSize
            emitter.onNext(status)

            readLen = fileInStream.read(buffer)
        }

        emitter.onComplete()

        using {

        }
    }
}