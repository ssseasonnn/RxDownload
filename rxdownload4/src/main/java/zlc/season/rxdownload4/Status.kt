package zlc.season.rxdownload4

import zlc.season.rxdownload4.utils.formatSize
import java.io.File
import kotlin.math.roundToLong


class Status(
        downloadSize: Long = 0,
        totalSize: Long = 0,
        isChunked: Boolean = false,
        file: File
) {

    var downloadSize: Long = downloadSize
        internal set

    var totalSize: Long = totalSize
        internal set

    /**
     * 用于标识一个链接是否是分块下载, 如果该值为true, 那么totalSize为-1
     */
    var isChunked: Boolean = isChunked
        internal set

    /**
     * Return download file, if file exists
     */
    var file: File = file
        internal set

    /**
     * Return total size str. eg: 10M
     */
    fun totalSizeStr(): String {
        return formatSize(totalSize)
    }

    /**
     * Return download size str. eg: 3M
     */
    fun downloadSizeStr(): String {
        return formatSize(downloadSize)
    }

    /**
     * Return percent number.
     */
    fun percent(): Double {
        if (isChunked) {
            throw IllegalStateException("Chunked can not get percent!")
        }

        val tmp = if (totalSize == 0L) {
            0.0
        } else {
            downloadSize * 1.0 / totalSize
        }

        tmp.roundToLong()

        return String.format("%.2f", tmp).toDouble()
    }

    /**
     * Return percent string.
     */
    fun percentStr(): String {
        return percent().toString()
    }

    override fun toString(): String {
        return "[$downloadSize/$totalSize] - ${percentStr()}"
    }
}