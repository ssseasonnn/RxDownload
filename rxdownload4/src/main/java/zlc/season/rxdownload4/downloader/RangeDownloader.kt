package zlc.season.rxdownload4.downloader

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiConsumer
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import retrofit2.Response
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.task.TaskInfo
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

    override fun download(taskInfo: TaskInfo, response: Response<ResponseBody>): Flowable<Progress> {
        file = taskInfo.task.getFile()
        shadowFile = file.shadow()
        tmpFile = file.tmp()

        beforeDownload(taskInfo, response)

        return if (alreadyDownloaded) {
            Flowable.just(Progress(
                    downloadSize = response.contentLength(),
                    totalSize = response.contentLength()
            ))
        } else {
            startDownload(taskInfo, response)
        }
    }

    private fun beforeDownload(taskInfo: TaskInfo, response: Response<ResponseBody>) {
        if (file.exists()) {
            if (taskInfo.validator.validate(file, response)) {
                alreadyDownloaded = true
            } else {
                file.deleteOnExit()
                createFiles(response, taskInfo)
            }
        } else {
            if (shadowFile.exists() && tmpFile.exists()) {

                rangeTmpFile = RangeTmpFile(tmpFile)

                if (!rangeTmpFile.read(response, taskInfo)) {
                    createFiles(response, taskInfo)
                }
            } else {
                createFiles(response, taskInfo)
            }
        }
    }

    private fun createFiles(response: Response<ResponseBody>, taskInfo: TaskInfo) {
        tmpFile.recreate {
            shadowFile.recreate {
                rangeTmpFile = RangeTmpFile(tmpFile)
                rangeTmpFile.write(response, taskInfo)
            }
        }
    }


    private fun startDownload(taskInfo: TaskInfo, response: Response<ResponseBody>): Flowable<Progress> {
        val url = response.url()
        val (downloadSize, totalSize) = rangeTmpFile.lastProgress()

        val progress = Progress(
                downloadSize = downloadSize,
                totalSize = totalSize
        )

        val sources = mutableListOf<Flowable<Long>>()

        rangeTmpFile.undoneSegments()
                .mapTo(sources) {
                    InnerDownloader(url, it, shadowFile, tmpFile, taskInfo.request).download()
                }

        return Flowable.mergeDelayError(sources, taskInfo.maxConCurrency)
                .map {
                    progress.apply {
                        this.downloadSize += it
                    }
                }
                .doOnComplete {
                    shadowFile.renameTo(file)
                    tmpFile.delete()
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
            private val tmpFile: File,
            private val request: Request
    ) {
        fun download(): Flowable<Long> {
            return Flowable.just(segment)
                    .subscribeOn(Schedulers.io())
                    .map { mapOf("Range" to "bytes=${it.current}-${it.end}") }
                    .flatMap { request.get(url, it) }
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
                    BiConsumer<InternalState, Emitter<Long>> { internalState, emitter ->
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
                    Consumer {
                        it.source.closeQuietly()
                    })
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

            shadowFileChannel.closeQuietly()
            tmpFileChannel.closeQuietly()

            return InternalState(
                    source,
                    shadowFileBuffer,
                    tmpFileBuffer,
                    downloadSize = segment.current
            )
        }
    }
}