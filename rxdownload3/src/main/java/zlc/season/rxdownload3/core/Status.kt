package zlc.season.rxdownload3.core

import zlc.season.rxdownload3.helper.formatSize
import java.text.NumberFormat.getPercentInstance


data class Status(var downloadSize: Long = 0L,
                  var totalSize: Long = 0L) {

    var chunkFlag: Boolean = false
    var flag: Int = SUSPEND

    fun toChunked(): Status {
        this.chunkFlag = true
        return this
    }

    fun toSucceed(): Status {
        this.flag = SUCCEED
        return this
    }

    fun toFailed(): Status {
        this.flag = FAILED
        return this
    }

    fun toSuspend(): Status {
        this.flag = SUSPEND
        return this
    }

    fun toWaiting(): Status {
        this.flag = WAITING
        return this
    }

    fun toDownloading(): Status {
        this.flag = DOWNLOADING
        return this
    }

    fun formatTotalSize(): String {
        return formatSize(totalSize)
    }

    fun formatDownloadSize(): String {
        return formatSize(downloadSize)
    }

    fun formatString(): String {
        return formatDownloadSize() + "/" + formatTotalSize()
    }

    fun percent(): String {
        val percent: String
        val result = if (totalSize == 0L) {
            0.0
        } else {
            downloadSize * 1.0 / totalSize
        }
        val nf = getPercentInstance()
        nf.minimumFractionDigits = 2
        percent = nf.format(result)
        return percent
    }

    companion object {
        val SUSPEND = 0x101
        val WAITING = 0x102
        val DOWNLOADING = 0x103
        val SUCCEED = 0x104
        val FAILED = 0x105

        fun isSuspend(status: Status): Boolean {
            return status.flag == SUSPEND
        }

        fun isWaiting(status: Status): Boolean {
            return status.flag == WAITING
        }

        fun isDownloading(status: Status): Boolean {
            return status.flag == DOWNLOADING
        }

        fun isSucceed(status: Status): Boolean {
            return status.flag == SUCCEED
        }

        fun isFailed(status: Status): Boolean {
            return status.flag == FAILED
        }
    }
}




