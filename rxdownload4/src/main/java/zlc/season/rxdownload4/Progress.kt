package zlc.season.rxdownload4

import zlc.season.rxdownload4.utils.formatSize
import zlc.season.rxdownload4.utils.ratio


class Progress(
        downloadSize: Long = 0,
        totalSize: Long = 0,
        isChunked: Boolean = false
) {

    var downloadSize: Long = downloadSize
        internal set

    var totalSize: Long = totalSize
        internal set
        get() {
            if (isChunked) {
                throw IllegalStateException("Chunked can not get totalSize!")
            }
            return field
        }

    /**
     * 用于标识一个链接是否是分块下载, 如果该值为true, 那么totalSize为-1
     */
    var isChunked: Boolean = isChunked
        internal set

    /**
     * Return total size str. eg: 10M
     */
    fun totalSizeStr(): String {
        return totalSize.formatSize()
    }

    /**
     * Return download size str. eg: 3M
     */
    fun downloadSizeStr(): String {
        return downloadSize.formatSize()
    }

    /**
     * Return percent number.
     */
    fun percent(): Double {
        check(!isChunked) { "Chunked can not get percent!" }

        return downloadSize ratio totalSize
    }

    /**
     * Return percent string.
     */
    fun percentStr(): String {
        return "${percent()}%"
    }
}