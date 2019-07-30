package zlc.season.rxdownload4

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import okhttp3.ResponseBody
import okio.Okio
import retrofit2.Response
import java.io.File

class NormalDownload(private val file: File) : DownloadType {
    private val shadowFile = file.shadow()

    var waitCount = 0

    override fun download(response: Response<ResponseBody>): Flowable<Status> {
        val body = response.body() ?: throw RuntimeException("Response body is NULL")

        val byteSize = 8192L

        var downloadSize = 0L
        val totalSize = body.contentLength()

        val status = Status(totalSize = totalSize)

        return Flowable.create({ emitter ->
            try {
                emitter.requested().log()

                body.source().use { source ->
                    Okio.buffer(Okio.sink(shadowFile)).use { sink ->
                        val buffer = sink.buffer()
                        var readLen = source.read(buffer, byteSize)

                        readLen.log()

                        while (readLen != -1L && !emitter.isCancelled) {
                            downloadSize += readLen

                            status.downloadSize = downloadSize
                            "download onnext".log()
                            emitter.onNext(status)

                            readLen = source.read(buffer, byteSize)
//                            downloadSize.log()

//                            Thread.sleep(500)
                            while (emitter.requested() == 0L) {
                                emitter.requested().log()
                                waitCount++
                                "wait: $waitCount".log()
                                Thread.sleep(5000)
                                if (emitter.isCancelled) {
                                    break
                                }
                            }
                        }

                        if (!emitter.isCancelled) {
                            shadowFile.renameTo(file)
                            emitter.onComplete()
                        }
                    }
                }
            } catch (t: Throwable) {
                if (!emitter.isCancelled) {
                    emitter.onError(t)
                }
            }
        }, BackpressureStrategy.BUFFER)
    }
}