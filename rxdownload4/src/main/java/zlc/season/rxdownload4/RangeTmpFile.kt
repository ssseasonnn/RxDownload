package zlc.season.rxdownload4

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.decodeHex
import okio.Okio
import retrofit2.Response
import zlc.season.rxdownload4.RangeTmpFile.Segment.Companion.SEGMENT_SIZE
import zlc.season.rxdownload4.utils.contentLength
import zlc.season.rxdownload4.utils.sliceCount
import java.io.File

class RangeTmpFile(private val file: File, response: Response<*>) {
    private val totalSize = response.contentLength()
    private val totalSegments = response.sliceCount()

    val header = FileHeader()
    val content = FileContent()

    fun write() {
        Okio.buffer(Okio.sink(file)).use {
            header.write(it, totalSize, totalSegments)
            content.write(it, totalSize, totalSegments)
        }
    }

    fun read() {
        Okio.buffer(Okio.source(file)).use {
            header.read(it)
            content.read(it, totalSegments)
        }
    }

    fun check(): Boolean {
        return header.check(totalSize)
    }


//    fun getSegments(): List<Segment> {
//        return fileStructure.segments
//    }

    /**
     * return segment start byte index
     */
    fun indexOf(segment: Segment): Long {
        return header.sizeOf() + SEGMENT_SIZE * segment.index
    }

//    fun currentStatus(): Status {
//        var downloadSize = 0L
//        val totalSize = fileStructure.totalSize
//
//        val segments = getSegments()
//        segments.forEach {
//            downloadSize += (it.current - it.start)
//        }
//
//        status.downloadSize = downloadSize
//        status.totalSize = totalSize
//
//        return status
//    }

    class FileHeader(
            private var totalSize: Long = 0L,
            private var totalSegments: Long = 0L
    ) {

        companion object {
            const val FILE_HEADER_MAGIC_NUMBER: String = "a1b2c3d4e5f6a1"
        }

        private val hex = decodeHex(FILE_HEADER_MAGIC_NUMBER)

        fun sizeOf() = hex.size() + 16L

        fun write(sink: BufferedSink, totalSize: Long, totalSegments: Long) {
            this.totalSize = totalSize
            this.totalSegments = totalSegments

            sink.write(hex)
            sink.writeLong(totalSize)
            sink.writeLong(totalSegments)
        }

        fun read(source: BufferedSource) {
            val header = source.readByteString(hex.size().toLong()).hex()
            if (header != FILE_HEADER_MAGIC_NUMBER) {
                throw RuntimeException("not a tmp file")
            }
            totalSize = source.readLong()
            totalSegments = source.readLong()
        }

        fun check(totalSize: Long): Boolean {
            return this.totalSize == totalSize
        }
    }

    class FileContent {
        val segments = mutableListOf<Segment>()

        fun write(sink: BufferedSink, totalSize: Long, totalSegments: Long) {
            sliceSegments(totalSize, totalSegments)

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

        private fun sliceSegments(totalSize: Long, totalSegments: Long) {
            segments.clear()

            var start = 0L

            for (i in 0 until totalSegments) {
                val end = if (i == totalSegments - 1) {
                    totalSize - 1
                } else {
                    start + DEFAULT_RANGE_SIZE - 1
                }

                segments.add(Segment(i, start, start, end))

                start += DEFAULT_RANGE_SIZE
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

        fun sizeOf(): Long {
            return end - current + 1
        }
    }
}