package zlc.season.rxdownload4

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.utils.*
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Callable

class RangeDownloader : Downloader {
    private lateinit var rangeTmpFile: RangeTmpFile
    private var alreadyDownloaded = false


    lateinit var file: File
    lateinit var shadowFile: File
    lateinit var tmpFile: File

    override fun download(response: Response<ResponseBody>): Flowable<Status> {
        prepare(response)

        if (alreadyDownloaded) {
            return Flowable.just(Status(response.contentLength(), response.contentLength()))
        } else {
            val url = response.raw().request().url().toString()

            val arrays = mutableListOf<Flowable<Any>>()

            rangeTmpFile.content.segments
                    .filter { !it.isComplete() }
                    .forEach { arrays.add(rangeDownload(url, it)) }

            return Flowable.mergeDelayError(arrays, 3)
                    .map {
                        Status()
                    }
                    .doOnComplete {
                        shadowFile.renameTo(file)
                    }
        }
    }

    private fun rangeDownload(url: String, segment: RangeTmpFile.Segment): Flowable<Any> {
        return Flowable.just(segment)
                .subscribeOn(Schedulers.io())
                .map { mapOf("Range" to "bytes=${it.current}-${it.end}").log() }
                .flatMap { Request().get(url, it) }
                .flatMap { save(segment, it) }
    }

    private fun prepare(response: Response<ResponseBody>) {
        file = response.file()
        shadowFile = file.shadow()
        tmpFile = file.tmp()

        if (file.exists()) {
            if (tmpFile.exists()) {
                rangeTmpFile = RangeTmpFile(tmpFile, response)
                rangeTmpFile.read()
                if (rangeTmpFile.check()) {
                    //file is valid
                    alreadyDownloaded = true
                } else {
                    file.deleteOnExit()
                    recreate(tmpFile, shadowFile, response)
                }
            } else {
                file.deleteOnExit()
                recreate(tmpFile, shadowFile, response)
            }
        } else {
            if (shadowFile.exists() && tmpFile.exists()) {
                rangeTmpFile = RangeTmpFile(tmpFile, response)
                rangeTmpFile.read()
                if (rangeTmpFile.check()) {

                } else {
                    recreate(tmpFile, shadowFile, response)
                }
            } else {
                recreate(tmpFile, shadowFile, response)
            }
        }
    }

    private fun recreate(
            tmpFile: File,
            shadowFile: File,
            response: Response<ResponseBody>
    ) {
        tmpFile.deleteOnExit()
        shadowFile.deleteOnExit()

        val tmpCreated = tmpFile.createNewFile()
        val shadowCreated = shadowFile.createNewFile()

        if (tmpCreated && shadowCreated) {
            //begin
            rangeTmpFile = RangeTmpFile(tmpFile, response)
            rangeTmpFile.write()
        }
    }

    class InternalState(
            val source: InputStream,
            val shadowFileBuffer: MappedByteBuffer,
            val tmpFileBuffer: MappedByteBuffer,
            val buffer: ByteArray = ByteArray(8192),
            var downloadSize: Long = 0
    )

    fun save(segment: RangeTmpFile.Segment, response: Response<ResponseBody>): Flowable<Long> {
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
                            emitter.onNext(downloadSize)
                        }
                    }
                },
                Consumer {

                })
    }

    private fun initialState(body: ResponseBody, segment: RangeTmpFile.Segment): InternalState {
        val source = body.byteStream()
        val shadowFileChannel = RandomAccessFile(shadowFile, "rw").channel
        val tmpFileChannel = RandomAccessFile(tmpFile, "rw").channel

        val tmpFileBuffer = tmpFileChannel.map(
                FileChannel.MapMode.READ_WRITE,
                rangeTmpFile.indexOf(segment),
                RangeTmpFile.Segment.SEGMENT_SIZE
        )


        val shadowFileBuffer = shadowFileChannel.map(
                FileChannel.MapMode.READ_WRITE,
                segment.current,
                segment.sizeOf()
        )

        shadowFileChannel.close()
        tmpFileChannel.close()

        return InternalState(source, shadowFileBuffer, tmpFileBuffer, downloadSize = segment.current)
    }

    class ShadowRangeFile() {

    }
}