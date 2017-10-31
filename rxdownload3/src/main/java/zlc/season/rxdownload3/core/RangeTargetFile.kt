package zlc.season.rxdownload3.core

import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig.DOWNLOADING_FILE_SUFFIX
import zlc.season.rxdownload3.core.RangeTmpFile.Segment
import zlc.season.rxdownload3.helper.ANY
import java.io.File
import java.io.File.separator
import java.io.RandomAccessFile
import java.nio.channels.FileChannel.MapMode.READ_WRITE
import java.util.concurrent.TimeUnit.MILLISECONDS


class RangeTargetFile(val mission: RealMission) {
    private val realFileDirPath = mission.actual.savePath
    private val realFilePath = realFileDirPath + separator + mission.actual.saveName

    private val shadowFilePath = realFilePath + DOWNLOADING_FILE_SUFFIX

    private val realFile = File(realFilePath)
    private val shadowFile = File(shadowFilePath)

    private val MODE = "rw"
    private val BUFFER_SIZE = 8192

    init {
        val dir = File(realFileDirPath)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }
    }

    fun isFinish(): Boolean {
        return realFile.exists()
    }

    fun isShadowExists(): Boolean {
        return shadowFile.exists()
    }

    fun createShadowFile() {
        val file = RandomAccessFile(shadowFile, MODE)
        file.setLength(mission.totalSize)
    }

    fun realFile(): File {
        return realFile
    }

    fun rename() {
        shadowFile.renameTo(realFile)
    }

    fun save(response: Response<ResponseBody>, segment: Segment, tmpFile: RangeTmpFile): Flowable<Any> {
        val respBody = response.body() ?: throw RuntimeException("Response body is NULL")
        val period = (1000 / DownloadConfig.fps).toLong()

        return Flowable.create<Any>({
            val buffer = ByteArray(BUFFER_SIZE)

            respBody.byteStream().use { source ->
                RandomAccessFile(shadowFile, MODE).use { target ->
                    RandomAccessFile(tmpFile.getFile(), MODE).use { tmp ->
                        target.channel.use { targetChannel ->
                            tmp.channel.use { tmpChannel ->

                                val segmentBuffer = tmpChannel.map(
                                        READ_WRITE,
                                        tmpFile.getPosition(segment),
                                        Segment.SEGMENT_SIZE
                                )

                                var readLen = source.read(buffer)

                                while (readLen != -1 && !it.isCancelled) {

                                    val targetBuffer = targetChannel.map(
                                            READ_WRITE,
                                            segment.current,
                                            readLen.toLong()
                                    )

                                    segment.current += readLen

                                    targetBuffer.put(buffer, 0, readLen)
                                    segmentBuffer.putLong(16, segment.current)

                                    it.onNext(ANY)
                                    readLen = source.read(buffer)
                                }

                                it.onComplete()
                            }
                        }
                    }
                }
            }
        }, BUFFER).sample(period, MILLISECONDS, true)
    }

    fun delete() {
        if (shadowFile.exists()) shadowFile.delete()
        if (realFile.exists()) realFile.delete()
    }

    fun isExists(): Boolean {
        return realFile.exists() || shadowFile.exists()
    }
}


