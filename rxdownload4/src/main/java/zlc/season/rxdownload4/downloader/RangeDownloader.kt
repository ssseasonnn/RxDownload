package zlc.season.rxdownload4.downloader

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.Status
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.*
import java.io.File
import java.io.InputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode.READ_WRITE
import java.util.concurrent.Callable

class RangeDownloader : Downloader {
    private var alreadyDownloaded = false

    private lateinit var file: File
    private lateinit var shadowFile: File
    private lateinit var tmpFile: File
    private lateinit var rangeTmpFile: RangeTmpFile

    override fun download(task: Task, response: Response<ResponseBody>): Flowable<Status> {
        file = response.file(task)
        shadowFile = file.shadow()
        tmpFile = file.tmp()

        beforeDownload(task, response)

        return if (alreadyDownloaded) {
            Flowable.just(Status(
                    downloadSize = response.contentLength(),
                    totalSize = response.contentLength()
            ))
        } else {
            startDownload(task, response)
        }
    }

    private fun beforeDownload(task: Task, response: Response<ResponseBody>) {
        if (file.exists()) {
            if (task.validator.validate(file, response)) {
                alreadyDownloaded = true
            } else {
                file.deleteOnExit()
                createFiles(response, task)
            }
        } else {
            if (shadowFile.exists() && tmpFile.exists()) {

                rangeTmpFile = RangeTmpFile(tmpFile)

                if (!rangeTmpFile.read(response, task)) {
                    createFiles(response, task)
                }
            } else {
                createFiles(response, task)
            }
        }
    }

    private fun createFiles(response: Response<ResponseBody>, task: Task) {
        tmpFile.recreate {
            shadowFile.recreate {
                rangeTmpFile = RangeTmpFile(tmpFile)
                rangeTmpFile.write(response, task)
            }
        }
    }


    private fun startDownload(task: Task, response: Response<ResponseBody>): Flowable<Status> {
        val url = response.url()
        val status = rangeTmpFile.lastStatus()

        val sources = mutableListOf<Flowable<Long>>()

        rangeTmpFile.undoneSegments()
                .forEach {
                    sources.add(
                            InnerDownloader(url, it, shadowFile, tmpFile).download()
                    )
                }

        return Flowable.mergeDelayError(sources, task.maxConCurrency)
                .map {
                    status.apply {
                        downloadSize += it
                    }
                }
                .doOnComplete {
                    shadowFile.renameTo(file)
                }
    }

    class InternalState(
            val source: InputStream,
            val shadowFileBuffer: MappedByteBuffer,
            val tmpFileBuffer: MappedByteBuffer,
            val buffer: ByteArray = ByteArray(8192),
            var downloadSize: Long = 0
    )

    class InnerDownloader(
            private val url: String,
            private val segment: RangeTmpFile.Segment,
            private val shadowFile: File,
            private val tmpFile: File
    ) {
        fun download(): Flowable<Long> {
            return Flowable.just(segment)
                    .subscribeOn(Schedulers.io())
                    .map { mapOf("Range" to "bytes=${it.current}-${it.end}").log() }
                    .flatMap { Request().get(url, it) }
                    .flatMap { rangeSave(segment, it) }
        }

        private fun rangeSave(
                segment: RangeTmpFile.Segment,
                response: Response<ResponseBody>
        ): Flowable<Long> {

            val body = response.body() ?: throw RuntimeException("Response body is NULL")

            return Flowable.generate(
                    Callable {
                        initialState(body, segment)
                    },
                    BiFunction<InternalState, Emitter<Long>, InternalState> { internalState, emitter ->
                        internalState.apply {
                            val readLen = source.read(buffer)

                            if (readLen == -1) {
                                shadowFileBuffer.force()
                                tmpFileBuffer.force()
                                emitter.onComplete()
                            } else {
                                shadowFileBuffer.put(buffer, 0, readLen)
                                tmpFileBuffer.putLong(16, downloadSize)

                                downloadSize += readLen
                                emitter.onNext(readLen.toLong())
                            }
                        }
                    },
                    Consumer {})
        }

        private fun initialState(
                body: ResponseBody,
                segment: RangeTmpFile.Segment
        ): InternalState {
            val source = body.byteStream()
            val shadowFileChannel = shadowFile.channel()
            val tmpFileChannel = tmpFile.channel()

            val tmpFileBuffer = tmpFileChannel.map(
                    READ_WRITE,
                    segment.startByte(),
                    RangeTmpFile.Segment.SEGMENT_SIZE
            )


            val shadowFileBuffer = shadowFileChannel.map(
                    READ_WRITE,
                    segment.current,
                    segment.remainSize()
            )

            shadowFileChannel.safeClose()
            tmpFileChannel.safeClose()

            return InternalState(
                    source,
                    shadowFileBuffer,
                    tmpFileBuffer,
                    downloadSize = segment.current
            )
        }
    }
}