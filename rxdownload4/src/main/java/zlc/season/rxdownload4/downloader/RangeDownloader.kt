package zlc.season.rxdownload4.downloader

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.DEFAULT_MAX_CONCURRENCY
import zlc.season.rxdownload4.DEFAULT_VALIDATOR
import zlc.season.rxdownload4.Request
import zlc.season.rxdownload4.Status
import zlc.season.rxdownload4.utils.*
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Callable

class RangeDownloader : Downloader {
    private var alreadyDownloaded = false

    private lateinit var file: File
    private lateinit var shadowFile: File
    private lateinit var tmpFile: File
    private lateinit var rangeTmpFile: RangeTmpFile

    override fun download(response: Response<ResponseBody>): Flowable<Status> {
        file = response.file()
        shadowFile = file.shadow()
        tmpFile = file.tmp()

        beforeDownload(response)

        return if (alreadyDownloaded) {
            val totalSize = response.contentLength()
            Flowable.just(Status(totalSize, totalSize))
        } else {
            startDownload(response)
        }
    }

    private fun beforeDownload(response: Response<ResponseBody>) {
        if (file.exists()) {
            if (DEFAULT_VALIDATOR.validate(file, response)) {
                alreadyDownloaded = true
            } else {
                file.deleteOnExit()
                createFiles(response)
            }
        } else {
            if (shadowFile.exists() && tmpFile.exists()) {
                rangeTmpFile = RangeTmpFile(tmpFile)
                if (!rangeTmpFile.read(response)) {
                    createFiles(response)
                }
            } else {
                createFiles(response)
            }
        }
    }

    private fun createFiles(response: Response<ResponseBody>) {
        tmpFile.deleteOnExit()
        shadowFile.deleteOnExit()

        val tmpCreated = tmpFile.createNewFile()
        val shadowCreated = shadowFile.createNewFile()

        if (tmpCreated && shadowCreated) {
            rangeTmpFile = RangeTmpFile(tmpFile)
            rangeTmpFile.write(response)
        } else {
            throw IllegalStateException("File create failed!")
        }
    }


    private fun startDownload(response: Response<ResponseBody>): Flowable<Status> {
        val url = response.url()
        val status = rangeTmpFile.lastStatus()

        val sources = mutableListOf<Flowable<Long>>()

        rangeTmpFile.undoneSegments()
                .forEach {
                    sources.add(
                            InnerRangerDownloader(url, it, shadowFile, tmpFile).download()
                    )
                }

        return Flowable.mergeDelayError(sources, DEFAULT_MAX_CONCURRENCY)
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

    class InnerRangerDownloader(
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

                    })
        }

        private fun initialState(
                body: ResponseBody,
                segment: RangeTmpFile.Segment
        ): InternalState {
            val source = body.byteStream()
            val shadowFileChannel = RandomAccessFile(shadowFile, "rw").channel
            val tmpFileChannel = RandomAccessFile(tmpFile, "rw").channel

            val tmpFileBuffer = tmpFileChannel.map(
                    FileChannel.MapMode.READ_WRITE,
                    segment.startByte(),
                    RangeTmpFile.Segment.SEGMENT_SIZE
            )


            val shadowFileBuffer = shadowFileChannel.map(
                    FileChannel.MapMode.READ_WRITE,
                    segment.current,
                    segment.remainSize()
            )

            shadowFileChannel.close()
            tmpFileChannel.close()

            return InternalState(source, shadowFileBuffer, tmpFileBuffer, downloadSize = segment.current)
        }
    }
}