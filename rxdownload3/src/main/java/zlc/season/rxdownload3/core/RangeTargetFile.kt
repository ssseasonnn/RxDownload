package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.core.RangeTmpFile.Segment
import java.io.File
import java.io.File.separator
import java.io.RandomAccessFile
import java.nio.channels.FileChannel.MapMode.READ_WRITE


class RangeTargetFile(val mission: RealMission) {
    private val fileDirPath = mission.actual.savePath
    private val filePath = fileDirPath + separator + mission.actual.fileName
    private val file = File(filePath)

    private val MODE = "rw"
    private val BUFFER_SIZE = 8192

    init {
        val dir = File(fileDirPath)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }

        if (!file.exists()) {
            file.createNewFile()
        } else {
            if (mission.actual.forceReDownload) {
                file.delete()
                file.createNewFile()
            }
        }
    }

    fun save(response: Response<ResponseBody>, segment: Segment, tmpFile: RangeTmpFile) {
        val respBody = response.body() ?: throw RuntimeException("Response body is NULL")

        val buffer = ByteArray(BUFFER_SIZE)

        respBody.byteStream().use { source ->
            RandomAccessFile(file, MODE).use { target ->
                RandomAccessFile(tmpFile.getFile(), MODE).use { tmp ->
                    target.channel.use { targetChannel ->
                        tmp.channel.use { tmpChannel ->

                            val targetBuffer = targetChannel.map(
                                    READ_WRITE,
                                    segment.current,
                                    segment.size()
                            )

                            val segmentBuffer = tmpChannel.map(
                                    READ_WRITE,
                                    tmpFile.getPosition(segment),
                                    Segment.SEGMENT_SIZE
                            )

                            var readLen = source.read(buffer)

                            while (readLen != -1) {
                                segment.current += readLen

                                targetBuffer.put(buffer, 0, readLen)
                                segmentBuffer.position(16)
                                segmentBuffer.putLong(segment.current)

                                mission.emitDownloadEvent(tmpFile.currentStatus())
                                readLen = source.read(buffer)
                            }
                        }
                    }
                }
            }
        }
    }
}


