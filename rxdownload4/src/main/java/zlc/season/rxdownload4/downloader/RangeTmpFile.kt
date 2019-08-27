package zlc.season.rxdownload4.downloader

import okio.*
import okio.ByteString.Companion.decodeHex
import retrofit2.Response
import zlc.season.rxdownload4.task.TaskInfo
import zlc.season.rxdownload4.utils.contentLength
import zlc.season.rxdownload4.utils.sliceCount
import java.io.File

class RangeTmpFile(private val tmpFile: File) {
    private val header = FileHeader()
    private val content = FileContent()

    fun write(response: Response<*>, taskInfo: TaskInfo) {
        val totalSize = response.contentLength()
        val totalSegments = response.sliceCount(taskInfo.rangeSize)

        tmpFile.sink().buffer().use {
            header.write(it, totalSize, totalSegments)
            content.write(it, totalSize, totalSegments, taskInfo.rangeSize)
        }
    }

    fun read(response: Response<*>, taskInfo: TaskInfo): Boolean {
        val totalSize = response.contentLength()
        val totalSegments = response.sliceCount(taskInfo.rangeSize)

        tmpFile.source().buffer().use {
            header.read(it)
            content.read(it, header.totalSegments)
        }
        return header.check(totalSize, totalSegments)
    }

    fun undoneSegments(): List<Segment> {
        return content.segments.filter { !it.isComplete() }
    }

    fun lastProgress(): Pair<Long, Long> {
        val totalSize = header.totalSize
        val downloadSize = content.downloadSize()

        return Pair(downloadSize, totalSize)
    }

    class FileHeader(
            var totalSize: Long = 0L,
            var totalSegments: Long = 0L
    ) {

        companion object {
            const val FILE_HEADER_MAGIC_NUMBER = "a1b2c3d4e5f6"

            //How to calc: ByteString.decodeHex(FILE_HEADER_MAGIC_NUMBER).size() = 6
            const val FILE_HEADER_MAGIC_NUMBER_SIZE = 6L

            //total header size
            const val FILE_HEADER_SIZE = FILE_HEADER_MAGIC_NUMBER_SIZE + 16L
        }

        fun write(sink: BufferedSink, totalSize: Long, totalSegments: Long) {
            this.totalSize = totalSize
            this.totalSegments = totalSegments

            sink.apply {
                write(FILE_HEADER_MAGIC_NUMBER.decodeHex())
                writeLong(totalSize)
                writeLong(totalSegments)
            }
        }

        fun read(source: BufferedSource) {
            val header = source.readByteString(FILE_HEADER_MAGIC_NUMBER_SIZE).hex()
            if (header != FILE_HEADER_MAGIC_NUMBER) {
                throw RuntimeException("not a tmp file")
            }
            totalSize = source.readLong()
            totalSegments = source.readLong()
        }

        fun check(totalSize: Long, totalSegments: Long): Boolean {
            return this.totalSize == totalSize &&
                    this.totalSegments == totalSegments
        }
    }

    class FileContent {
        val segments = mutableListOf<Segment>()

        fun write(
                sink: BufferedSink,
                totalSize: Long,
                totalSegments: Long,
                rangeSize: Long
        ) {
            segments.clear()

            sliceSegments(totalSize, totalSegments, rangeSize)

            segments.forEach {
                it.write(sink)
            }
        }

        fun read(source: BufferedSource, totalSegments: Long) {
            segments.clear()
            for (i in 0 until totalSegments) {
                segments.add(Segment().read(source))
            }
        }

        fun downloadSize(): Long {
            var downloadSize = 0L
            segments.forEach {
                downloadSize += it.completeSize()
            }
            return downloadSize
        }

        private fun sliceSegments(totalSize: Long, totalSegments: Long, rangeSize: Long) {
            var start = 0L

            for (i in 0 until totalSegments) {
                val end = if (i == totalSegments - 1) {
                    totalSize - 1
                } else {
                    start + rangeSize - 1
                }

                segments.add(Segment(i, start, start, end))

                start += rangeSize
            }
        }
    }

    class Segment(
            var index: Long = 0L,
            var start: Long = 0L,
            var current: Long = 0L,
            var end: Long = 0L
    ) {

        companion object {
            const val SEGMENT_SIZE = 32L //each Long is 8 bytes
        }

        fun write(sink: BufferedSink): Segment {
            sink.apply {
                writeLong(index)
                writeLong(start)
                writeLong(current)
                writeLong(end)
            }
            return this
        }

        fun read(source: BufferedSource): Segment {
            val buffer = Buffer()
            source.readFully(buffer, SEGMENT_SIZE)

            buffer.apply {
                index = readLong()
                start = readLong()
                current = readLong()
                end = readLong()
            }

            return this
        }

        fun isComplete() = (current - end) == 1L

        fun remainSize() = end - current + 1

        fun completeSize() = current - start

        /**
         * Return the starting position of the segment
         */
        fun startByte() = FileHeader.FILE_HEADER_SIZE + SEGMENT_SIZE * index
    }
}