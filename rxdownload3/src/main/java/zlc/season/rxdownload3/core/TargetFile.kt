package zlc.season.rxdownload3.core

import io.reactivex.FlowableEmitter
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.InputStream
import java.io.OutputStream


class TargetFile {
    lateinit var file: File

    fun save(emitter: FlowableEmitter<DownloadStatus>, response: Response<ResponseBody>) {
        var fileInStream: InputStream
        var fileOutSteam: OutputStream

        val buffer: ByteArray = kotlin.ByteArray(8)


        val respBody = response.body()
        if (respBody == null) {
            emitter.onError(RuntimeException("body is null"))
            return
        }

        fileInStream = respBody.byteStream()

    }
}