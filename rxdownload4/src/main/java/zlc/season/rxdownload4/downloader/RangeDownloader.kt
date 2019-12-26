package zlc.season.rxdownload4.downloader

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiConsumer
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.downloader.RangeTmpFile.Segment.Companion.SEGMENT_SIZE
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.task.TaskInfo
import zlc.season.rxdownload4.utils.*
import java.io.File
import java.io.InputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
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
        //make sure dir is exists
        val fileDir = taskInfo.task.getDir()
        if (!fileDir.exists() || !fileDir.isDirectory) {
            fileDir.mkdirs()
        }

        if (file.exists()) {
            if (taskInfo.validator.validate(file, response)) {
                alreadyDownloaded = true
            } else {
                file.delete()
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
            shadowFile.recreate(response.contentLength()) {
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
                    response.body()?.closeQuietly()
                }
    }

    class InternalState(
            val source: InputStream,
            val shadowChannel: FileChannel,
            val tmpFileChannel: FileChannel,
            var tmpFileBuffer: MappedByteBuffer,
            var shadowFileBuffer: MappedByteBuffer,
            var current: Long = 0
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
                    .map { mapOf("Range" to "bytes=${it.current}-${it.end}").log() }
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
                            val buffer = ByteArray(8192)
                            val readLen = source.read(buffer)

                            if (readLen == -1) {
                                emitter.onComplete()
                            } else {
                                shadowFileBuffer.put(buffer, 0, readLen)

                                current += readLen

                                tmpFileBuffer.putLong(16, current)

                                emitter.onNext(readLen.toLong())
                            }
                        }
                    },
                    Consumer {
                        it.tmpFileChannel.closeQuietly()
                        it.shadowChannel.closeQuietly()
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
                    SEGMENT_SIZE
            )

            val shadowFileBuffer = shadowFileChannel.map(
                    READ_WRITE,
                    segment.current,
                    segment.remainSize()
            )

            return InternalState(
                    source,
                    shadowFileChannel,
                    tmpFileChannel,
                    tmpFileBuffer,
                    shadowFileBuffer,
                    current = segment.current
            )
        }

    }
}