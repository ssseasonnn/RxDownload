package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import okio.Buffer
import okio.ByteString
import okio.Okio
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.RANGE_DOWNLOAD_SIZE
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel.MapMode.READ_WRITE

class RangeTmpFile(missionWrapper: MissionWrapper) : DownloadFile(missionWrapper) {
    private val TMP_FILE_HEADER_MAGIC_NUMBER = "A0B0C0D0E0F"
    private val MAGIC_NUMBER = ByteString.decodeHex(TMP_FILE_HEADER_MAGIC_NUMBER)

    private val TMP_DIR_SUFFIX = ".TMP"
    private val TMP_FILE_SUFFIX = ".tmp"

    private val tmpDirPath = missionWrapper.realPath + File.separator + TMP_DIR_SUFFIX
    private val tmpFilePath = tmpDirPath + File.separator + missionWrapper.realFileName + TMP_FILE_SUFFIX
    val file = File(tmpFilePath)


    private val MODE = "rw"

    init {
        val dir = File(tmpDirPath)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }

        if (!file.exists()) {
            file.createNewFile()

            Okio.buffer(Okio.sink(file)).use {
                val header = MAGIC_NUMBER
                it.write(header)
                it.writeLong(0L)
                it.writeLong(missionWrapper.contentLength)

                val remainder = missionWrapper.contentLength % RANGE_DOWNLOAD_SIZE
                val total = if (remainder == 0L) {
                    missionWrapper.contentLength / RANGE_DOWNLOAD_SIZE
                } else {
                    missionWrapper.contentLength / RANGE_DOWNLOAD_SIZE + 1
                }

                var start = 0L

                for (i in 0..total) {
                    it.writeLong(i)
                    it.writeLong(start)
                    it.writeLong(start + RANGE_DOWNLOAD_SIZE - 1)
                    start += RANGE_DOWNLOAD_SIZE
                }
            }
        }
    }

    fun write(resp: Response<ResponseBody>, segment: Segment) {
        RandomAccessFile(file, MODE).use {
            it.channel.use {
                val position = MAGIC_NUMBER.size() + Segment.SEGMENT_SIZE * segment.index
                val byteBuffer = it.map(READ_WRITE, position, Segment.SEGMENT_SIZE)

                byteBuffer.position(4)
                byteBuffer.putLong(segment.start)
            }
        }
    }

    fun write(segment: Segment) {
        RandomAccessFile(file, MODE).use {
            it.channel.use {
                val position = MAGIC_NUMBER.size() + Segment.SEGMENT_SIZE * segment.index
                val byteBuffer = it.map(READ_WRITE, position, Segment.SEGMENT_SIZE)

                byteBuffer.position(4)
                byteBuffer.putLong(segment.start)
            }
        }
    }

    fun isFinish(): Boolean {
        Okio.buffer(Okio.source(file)).use {
            val header = it.readByteString(MAGIC_NUMBER.size().toLong())
            if (header != MAGIC_NUMBER) {
                throw Exception("Not a tmp file")
            }

            val currentSize = it.readLong()
            val totalSize = it.readLong()

            return currentSize == totalSize
        }
    }

    fun read(): List<Segment> {
        val result = mutableListOf<Segment>()

        Okio.buffer(Okio.source(file)).use {
            val header = it.readByteString(MAGIC_NUMBER.size().toLong())
            if (header != MAGIC_NUMBER) {
                throw Exception("Not a tmp file")
            }

            val currentSize = it.readLong()
            val totalSize = it.readLong()

            while (true) {
                val buffer = Buffer()
                it.readFully(buffer, Segment.SEGMENT_SIZE)

                val index = buffer.readLong()
                val start = buffer.readLong()
                val end = buffer.readLong()

                val segment = Segment(index, start, end)
                result.add(segment)
            }
        }

        return result
    }
}