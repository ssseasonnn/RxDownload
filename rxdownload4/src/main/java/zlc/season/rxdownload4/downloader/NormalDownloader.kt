package zlc.season.rxdownload4.downloader

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.Flowable.generate
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import okhttp3.ResponseBody
import okio.*
import retrofit2.Response
import zlc.season.rxdownload4.Status
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.*
import java.io.File
import java.util.concurrent.Callable

class NormalDownloader : Downloader {
    private var alreadyDownloaded = false

    private lateinit var file: File
    private lateinit var shadowFile: File

    override fun download(task: Task, response: Response<ResponseBody>): Flowable<Status> {
        val body = response.body() ?: throw RuntimeException("Response body is NULL")

        file = response.file(task)

        shadowFile = file.shadow()

        beforeDownload(task, response)

        return if (alreadyDownloaded) {
            Flowable.just(Status(
                    downloadSize = response.contentLength(),
                    totalSize = response.contentLength(),
                    file = file
            ))
        } else {
            startDownload(body, Status(
                    totalSize = response.contentLength(),
                    isChunked = response.isChunked(),
                    file = file
            ))
        }
    }

    private fun beforeDownload(task: Task, response: Response<ResponseBody>) {
        if (file.exists()) {
            if (task.validator.validate(file, response)) {
                alreadyDownloaded = true
            } else {
                file.deleteOnExit()
                shadowFile.recreate()
            }
        } else {
            shadowFile.recreate()
        }
    }

    private fun startDownload(body: ResponseBody, status: Status): Flowable<Status> {

        return generate(
                Callable {
                    InternalState(
                            body.source(),
                            shadowFile.sink().buffer()
                    )
                },
                BiFunction<InternalState, Emitter<Status>, InternalState> { internalState, emitter ->
                    internalState.apply {
                        val readLen = source.read(buffer, 8192L)

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
            val sink: BufferedSink,
            val buffer: Buffer = sink.buffer
    )
}