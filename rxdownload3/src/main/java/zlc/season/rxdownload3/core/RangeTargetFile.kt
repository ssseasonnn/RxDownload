package zlc.season.rxdownload3.core

import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel


class RangeTargetFile(mission: RealMission) : DownloadFile(mission) {
    private val filePath = mission.realPath + File.separator + mission.realFileName
    private val file = File(filePath)

    private val MODE = "rw"

    val tmpFile = RangeTmpFile(mission)

    fun save(response: Response<ResponseBody>, segment: Segment) {
        val respBody = response.body()
        if (respBody == null) {
            mission.processor.onError(RuntimeException("body is null"))
            return
        }

        val buffer = ByteArray(8192)

        respBody.byteStream().use { source ->
            RandomAccessFile(file, MODE).use {
                it.channel.use {
                    val mappedByteBuffer = it.map(FileChannel.MapMode.READ_WRITE,
                            segment.start, segment.end - segment.start + 1)

                    var readLen = source.read(buffer)
                    while (readLen != -1) {
                        mappedByteBuffer.put(buffer, 0, readLen)
                        readLen = source.read(buffer)
                    }
                }
            }
        }
    }
}


