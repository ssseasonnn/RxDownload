package zlc.season.rxdownload3.core

import okio.*
import zlc.season.rxdownload3.core.DownloadConfig.RANGE_DOWNLOAD_SIZE
import java.io.File

class RangeTmpFile(mission: RealMission) : DownloadFile(mission) {
    private val TMP_DIR_SUFFIX = ".TMP"
    private val TMP_FILE_SUFFIX = ".tmp"

    private val tmpDirPath = mission.realPath + File.separator + TMP_DIR_SUFFIX
    private val tmpFilePath = tmpDirPath + File.separator + mission.realFileName + TMP_FILE_SUFFIX

    private val file = File(tmpFilePath)

    val fileHeader = FileHeader()
    val fileSegment = FileSegment()

    init {
        val dir = File(tmpDirPath)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }

        if (!file.exists()) {
            file.createNewFile()

            Okio.buffer(Okio.sink(file)).use {
                fileHeader.writeHeader(it)
                fileSegment.writeSegments(it)
            }
        } else {
            Okio.buffer(Okio.source(file)).use {
                fileHeader.readHeader(it)
                fileSegment.readSegments(fileHeader, it)
            }
        }
    }

    private fun calculateSegments(realMission: RealMission): Long {
        val remainder = realMission.contentLength % RANGE_DOWNLOAD_SIZE
        return if (remainder == 0L) {
            realMission.contentLength / RANGE_DOWNLOAD_SIZE
        } else {
            realMission.contentLength / RANGE_DOWNLOAD_SIZE + 1
        }
    }

    inner class FileHeader {
        private val FILE_HEADER_MAGIC_NUMBER = "a1b2c3d4e5f6"
        private val FILE_HEADER_MAGIC_NUMBER_HEX = ByteString.decodeHex(FILE_HEADER_MAGIC_NUMBER)

        var currentSize: Long = 0L
        var totalSize: Long = 0L
        var totalSegment: Long = 0L

        fun writeHeader(sink: BufferedSink) {
            totalSize = mission.contentLength
            totalSegment = calculateSegments(mission)

            sink.write(FILE_HEADER_MAGIC_NUMBER_HEX)
            sink.writeLong(0L)
            sink.writeLong(totalSize)
            sink.writeLong(totalSegment)
        }

        fun readHeader(source: BufferedSource) {
            checkFileHeader(source)
            currentSize = source.readLong()
            totalSize = source.readLong()
            totalSegment = source.readLong()
        }

        fun isFinish(): Boolean {
            return currentSize == totalSize
        }

        private fun checkFileHeader(source: BufferedSource) {
            val header = source.readByteString(FILE_HEADER_MAGIC_NUMBER_HEX.size().toLong()).hex()
            if (header != FILE_HEADER_MAGIC_NUMBER) {
                throw Exception("Not a tmp file")
            }
        }
    }

    inner class FileSegment {
        private val SEGMENT_SIZE = 24L //each Long is 8 bytes

        var segments = mutableListOf<Segment>()


        fun writeSegments(sink: BufferedSink) {
            var start = 0L
            val total = calculateSegments(mission)

            segments.clear()

            for (i in 0 until total) {
                val end = if (i == total - 1) {
                    mission.contentLength - 1
                } else {
                    start + RANGE_DOWNLOAD_SIZE - 1
                }

                segments.add(Segment(i, start, end).write(sink))

                start += RANGE_DOWNLOAD_SIZE
            }
        }

        fun readSegments(fileHeader: FileHeader, source: BufferedSource) {
            segments.clear()

            for (i in 0 until fileHeader.totalSegment) {
                val buffer = Buffer()
                source.readFully(buffer, SEGMENT_SIZE)

                val index = buffer.readLong()
                val start = buffer.readLong()
                val end = buffer.readLong()

                val segment = Segment(index, start, end)
                segments.add(segment)
            }
        }

        inner class Segment(val index: Long, val start: Long, val end: Long) {
            fun isComplete(): Boolean {
                return (start - end) == 1L
            }

            fun write(sink: BufferedSink): Segment {
                sink.writeLong(index)
                sink.writeLong(start)
                sink.writeLong(end)
                return this
            }
        }
    }
}