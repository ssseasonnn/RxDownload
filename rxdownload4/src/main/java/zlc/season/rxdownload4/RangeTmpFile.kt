package zlc.season.rxdownload4

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.decodeHex
import okio.Okio
import java.io.File

class RangeTmpFile(private val file: File, private val totalSize: Long) {
    val header = FileHeader()
    val content = FileContent()

    fun write() {
        val totalSegments = calculateTotalSegments(totalSize)
        header.totalSize = totalSize
        header.totalSegments = totalSegments

        content.slice(totalSize, totalSegments)

        Okio.buffer(Okio.sink(file)).use {
            header.write(it)
            content.write(it)
        }
    }

    fun read() {
        Okio.buffer(Okio.source(file)).use {
            header.read(it)
            content.read(it, header.totalSegments)
        }
    }

    fun check(): Boolean {
        return header.totalSize == totalSize
    }

    private fun calculateTotalSegments(totalSize: Long): Long {
        val remainder = totalSize % DEFAULT_RANGE_SIZE
        val result = totalSize / DEFAULT_RANGE_SIZE

        return if (remainder == 0L) {
            result
        } else {
            result + 1
        }
    }


//    fun getSegments(): List<Segment> {
//        return fileStructure.segments
//    }

//    fun getPosition(segment: Segment): Long {
//        return fileStructure.size() + SEGMENT_SIZE * segment.index
//    }
//
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
            var totalSize: Long = 0L,
            var totalSegments: Long = 0L
    ) {

        companion object {
            const val FILE_HEADER_MAGIC_NUMBER: String = "a1b2c3d4e5f6g7"
        }

        private val hex = decodeHex(FILE_HEADER_MAGIC_NUMBER)

        fun sizeOf() = hex.size() + 16L

        fun write(sink: BufferedSink) {
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
    }

    class FileContent {
        var segments = mutableListOf<Segment>()

        fun write(sink: BufferedSink) {
            segments.forEach {
                it.write(sink)
            }
        }

        fun read(source: BufferedSource, totalSegments: Long) {
            segments.clear()

            for (i in 0 until totalSegments) {
                val buffer = Buffer()
                source.readFully(buffer, Segment.SEGMENT_SIZE)

                val index = buffer.readLong()
                val start = buffer.readLong()
                val current = buffer.readLong()
                val end = buffer.readLong()

                segments.add(Segment(index, start, current, end))
            }
        }

        fun isFinish(): Boolean {
            if (segments.isEmpty()) {
                return false
            }

            return segments.any { it.isComplete() }
        }

        fun slice(totalSize: Long, totalSegments: Long) {
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
            private val index: Long,
            private val start: Long,
            var current: Long,
            private val end: Long
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

        fun sizeOf(): Long {
            return end - current + 1
        }
    }
}