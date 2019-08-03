package zlc.season.rxdownload4

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.Flowable.generate
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.BufferedSource
import okio.Okio.buffer
import okio.Okio.sink
import retrofit2.Response
import zlc.season.rxdownload4.utils.*
import java.util.concurrent.Callable

class NormalDownloader : Downloader {

    private val byteSize = 8192L

    class InternalState(
            val source: BufferedSource,
            val sink: BufferedSink
    )

    override fun download(response: Response<ResponseBody>): Flowable<Status> {
        val body = response.body() ?: throw RuntimeException("Response body is NULL")

        val file = response.file()
        val shadowFile = file.shadow()

        val status = Status(
                totalSize = response.contentLength(),
                isChunked = response.isChunked()
        )

        return generate(
                Callable {
                    InternalState(body.source(), buffer(sink(shadowFile)))
                },
                BiFunction<InternalState, Emitter<Status>, InternalState> { internalState, emitter ->
                    internalState.apply {
                        val readLen = source.read(sink.buffer(), byteSize)

                        if (readLen == -1L) {
                            sink.flush()
                            shadowFile.renameTo(file)
                            emitter.onComplete()
                        } else {
                            sink.emit()
                            emitter.onNext(status.apply {
                                downloadSize += readLen
                            })
                        }
                    }
                },
                Consumer {
                    it.apply {
                        sink.safeClose()
                        source.safeClose()
                    }
                })
    }
}