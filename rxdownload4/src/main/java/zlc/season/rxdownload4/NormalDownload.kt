package zlc.season.rxdownload4

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.BufferedSource
import okio.Okio
import retrofit2.Response
import java.io.Closeable
import java.io.File
import java.util.concurrent.Callable

class NormalDownload(private val file: File) : DownloadType {
    private val shadowFile = file.shadow()

    class InternalState(
            val source: BufferedSource,
            val sink: BufferedSink,
            var status: Status = Status()
    )

    override fun download(response: Response<ResponseBody>): Flowable<Status> {
        val body = response.body() ?: throw RuntimeException("Response body is NULL")

        val byteSize = 8192L

        val status = Status(totalSize = body.contentLength())

        return Flowable.generate(
                Callable {
                    val source = body.source()
                    val sink = Okio.buffer(Okio.sink(shadowFile))

                    return@Callable InternalState(source, sink, status)
                },
                BiFunction<InternalState, Emitter<Status>, InternalState> { internalState, emitter ->
                    val source = internalState.source
                    val sink = internalState.sink

                    val readLen = source.read(sink.buffer(), byteSize)
                    sink.emit()

                    if (readLen == -1L) {
                        sink.flush()
                        shadowFile.renameTo(file)
                        emitter.onComplete()
                    } else {
                        val last = internalState.status.downloadSize
                        val new = last + readLen
                        internalState.status.downloadSize = new
                        emitter.onNext(internalState.status)
                    }

                    return@BiFunction internalState
                },
                Consumer {
                    it.sink.safeClose()
                    it.source.safeClose()
                })
    }

    fun Closeable.safeClose() {
        try {
            close()
        } catch (ignore: Throwable) {

        }
    }
}