package zlc.season.rxdownload4.downloader

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.decodeHex
import okio.Okio
import retrofit2.Response
import zlc.season.rxdownload4.Status
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.contentLength
import zlc.season.rxdownload4.utils.sliceCount
import java.io.File

class RangeTmpFile(private val tmpFile: File) {
    private val header = FileHeader()
    private val content = FileContent()

    fun write(response: Response<*>, task: Task) {
        val totalSize = response.contentLength()
        val totalSegments = response.sliceCount(task.rangeSize)

        Okio.buffer(Okio.sink(tmpFile)).use {
            header.write(it, totalSize, totalSegments)
            content.write(it, totalSize, totalSegments, task)
        }
    }

    fun read(response: Response<*>, task: Task): Boolean {
        val totalSize = response.contentLength()
        val totalSegments = response.sliceCount(task.rangeSize)

        Okio.buffer(Okio.source(tmpFile)).use {
            header.read(it)
            content.read(it, totalSegments)
        }
        return header.check(totalSize, totalSegments) &&
                content.check(task)
    }

    fun undoneSegments(): List<Segment> {
        return content.segments.filter { !it.isComplete() }
    }

    fun lastStatus(): Status {
        var downloadSize = 0L
        val totalSize = header.totalSize

        val segments = content.segments
        segments.forEach {
            downloadSize += it.completeSize()
        }

        return Status(downloadSize, totalSize)
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
                write(decodeHex(FILE_HEADER_MAGIC_NUMBER))
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

        fun check(task: Task): Boolean {
            var isValid = true
            segments.forEach {
                if (it.size() != task.rangeSize) {
                    isValid = false
                }
            }
            return isValid
        }

        fun write(sink: BufferedSink, totalSize: Long, totalSegments: Long, task: Task) {
            sliceSegments(totalSize, totalSegments, task)

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

        fun isFinish(): Boolean {
            if (segments.isEmpty()) {
                return false
            }

            return segments.any { it.isComplete() }
        }

        private fun sliceSegments(totalSize: Long, totalSegments: Long, task: Task) {
            segments.clear()

            var start = 0L

            for (i in 0 until totalSegments) {
                val end = if (i == totalSegments - 1) {
                    totalSize - 1
                } else {
                    start + task.rangeSize - 1
                }

                segments.add(Segment(i, start, start, end))

                start += task.rangeSize
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

        fun size(): Long {
            return end - start
        }

        fun isComplete(): Boolean {
            return (current - end) == 1L
        }

        fun write(sink: BufferedSink): Segment {
            sink.writeLong(index)
            sink.writeLong(start)
            sink.writeLong(current)
            sink.writeLong(end)
            return this
        }

        fun read(source: BufferedSource): Segment {
            val buffer = Buffer()
            source.readFully(buffer, SEGMENT_SIZE)

            index = buffer.readLong()
            start = buffer.readLong()
            current = buffer.readLong()
            end = buffer.readLong()

            return this
        }

        fun remainSize(): Long {
            return end - current + 1
        }

        fun completeSize(): Long {
            return current - start
        }

        /**
         * Return the starting position of the segment
         */
        fun startByte(): Long {
            return FileHeader.FILE_HEADER_SIZE + SEGMENT_SIZE * index
        }
    }
}