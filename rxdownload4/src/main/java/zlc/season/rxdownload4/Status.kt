package zlc.season.rxdownload4

import java.text.NumberFormat

class Status(
        var downloadSize: Long = 0L,
        var totalSize: Long = 0L,

        //用于标识一个链接是否是分块下载, 如果该值为true, 那么totalSize为0
        var isChunked: Boolean = false
) {

    constructor(status: Status) : this(status.downloadSize, status.totalSize, status.isChunked)

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