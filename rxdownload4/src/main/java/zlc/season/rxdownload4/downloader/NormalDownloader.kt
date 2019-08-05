package zlc.season.rxdownload4.downloader

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
import zlc.season.rxdownload4.DEFAULT_VALIDATOR
import zlc.season.rxdownload4.Status
import zlc.season.rxdownload4.utils.*
import java.io.File
import java.util.concurrent.Callable

class NormalDownloader : Downloader {
    private var alreadyDownloaded = false

    private lateinit var file: File
    private lateinit var shadowFile: File

    override fun download(response: Response<ResponseBody>): Flowable<Status> {
        val body = response.body() ?: throw RuntimeException("Response body is NULL")

        file = response.file()
        shadowFile = file.shadow()

        beforeDownload(response)

        return if (alreadyDownloaded) {
            Flowable.just(Status(
                    downloadSize = response.contentLength(),
                    totalSize = response.contentLength()
            ))
        } else {
            startDownload(body, Status(
                    totalSize = response.contentLength(),
                    isChunked = response.isChunked()
            ))
        }
    }

    private fun beforeDownload(response: Response<ResponseBody>) {
        if (file.exists()) {
            if (DEFAULT_VALIDATOR.validate(file, response)) {
                alreadyDownloaded = true
            } else {
                file.deleteOnExit()
                createFile(shadowFile)
            }
        } else {
            createFile(shadowFile)
        }
    }

    private fun createFile(shadowFile: File) {
        shadowFile.deleteOnExit()
        val created = shadowFile.createNewFile()
        if (!created) {
            throw IllegalStateException("File create failed!")
        }
    }

    private fun startDownload(body: ResponseBody, status: Status): Flowable<Status> {

        return generate(
                Callable {
                    InternalState(
                            body.source(),
                            buffer(sink(shadowFile))
                    )
                },
                BiFunction<InternalState, Emitter<Status>, InternalState> { internalState, emitter ->
                    internalState.apply {
                        val readLen = source.read(sink.buffer(), 8192L)

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

    class InternalState(
            val source: BufferedSource,
            val sink: BufferedSink
    )
}