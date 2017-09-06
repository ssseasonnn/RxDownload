package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.core.RangeTmpFile.Segment
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel.MapMode.READ_WRITE


class RangeTargetFile(mission: RealMission) : DownloadFile(mission) {
    private val filePath = mission.realPath + File.separator + mission.realFileName
    private val file = File(filePath)
    private val tmp = RangeTmpFile(mission)

    private val MODE = "rw"
    private val BUFFER_SIZE = 8192

    fun getTmpFile(): RangeTmpFile {
        return tmp
    }

    fun save(response: Response<ResponseBody>, segment: Segment) {
        val respBody = response.body() ?: throw RuntimeException("Response body is NULL")

        val buffer = ByteArray(BUFFER_SIZE)

        respBody.byteStream().use { source ->
            RandomAccessFile(file, MODE).use { targetFile ->
                RandomAccessFile(tmp.getFile(), MODE).use { tmpFile ->
                    targetFile.channel.use { targetChannel ->
                        tmpFile.channel.use { tmpChannel ->
                            val targetBuffer = targetChannel.map(READ_WRITE,
                                    segment.start, segment.end - segment.start + 1)
                            val segmentBuffer = tmpChannel.map(READ_WRITE,
                                    tmp.getPosition(segment), Segment.SEGMENT_SIZE)

                            var readLen = source.read(buffer)
                            while (readLen != -1) {
                                segment.start += readLen
                                targetBuffer.put(buffer, 0, readLen)
                                segmentBuffer.position(8)
                                segmentBuffer.putLong(segment.start)

                                mission.processor.onNext(tmp.getDownloadStatus())
                                readLen = source.read(buffer)
                            }
                        }
                    }
                }
            }
        }
    }
}


