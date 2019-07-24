package zlc.season.rxdownload4

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import okhttp3.ResponseBody
import okio.Okio
import retrofit2.Response
import java.io.File

class NormalDownload(val file: File) {
    val shadowFile = file.shadowFile()

    fun save(response: Response<ResponseBody>): Flowable<Status> {
        val body = response.body() ?: throw RuntimeException("Response body is NULL")

        val byteSize = 8192L

        var downloadSize = 0L
        val totalSize = body.contentLength()

        val status = Status(totalSize = totalSize)

        return Flowable.create<Status>({ emitter ->
            body.source().use { source ->
                Okio.buffer(Okio.sink(shadowFile)).use { sink ->
                    val buffer = sink.buffer()
                    var readLen = source.read(buffer, byteSize)

                    while (readLen != -1L && !emitter.isCancelled) {
                        downloadSize += readLen

                        status.downloadSize = downloadSize
                        emitter.onNext(status)

                        readLen = source.read(buffer, byteSize)
                    }

                    if (!emitter.isCancelled) {
                        emitter.onComplete()
                    }
                }
            }
        }, BackpressureStrategy.BUFFER)

    }
}