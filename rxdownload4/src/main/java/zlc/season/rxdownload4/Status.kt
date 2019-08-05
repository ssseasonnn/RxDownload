package zlc.season.rxdownload4

import java.text.NumberFormat

class Status(downloadSize: Long = 0, totalSize: Long = 0, isChunked: Boolean = false) {

    var downloadSize: Long = downloadSize
        internal set

    var totalSize: Long = totalSize
        internal set

    //用于标识一个链接是否是分块下载, 如果该值为true, 那么totalSize为-1
    var isChunked: Boolean = isChunked
        internal set


//    fun formatTotalSize(): String {
//        return formatSize(totalSize)
//    }
//
//    fun formatDownloadSize(): String {
//        return formatSize(downloadSize)
//    }

//    fun formatString(): String {
//        return formatDownloadSize() + "/" + formatTotalSize()
//    }

    fun percent(): String {
        val percent: String
        val result = if (totalSize == 0L) {
            0.0
        } else {
            downloadSize * 1.0 / totalSize
        }
        val nf = NumberFormat.getPercentInstance()
        nf.minimumFractionDigits = 2
        percent = nf.format(result)
        return percent
    }

    override fun toString(): String {
        return "[$downloadSize/$totalSize] - ${percent()}"
    }
}