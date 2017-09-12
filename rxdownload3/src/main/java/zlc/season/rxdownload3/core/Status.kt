package zlc.season.rxdownload3.core

import zlc.season.rxdownload3.helper.formatSize
import java.text.NumberFormat.getPercentInstance


open class Status(var chunkFlag: Boolean = false,
                  var downloadSize: Long = 0L,
                  var totalSize: Long = 0L) {

    constructor(status: Status) : this() {
        this.chunkFlag = status.chunkFlag
        this.downloadSize = status.downloadSize
        this.totalSize = status.totalSize
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
}

class Suspend(status: Status = Status()) : Status(status)

class Waiting(status: Status) : Status(status)

class Downloading(status: Status) : Status(status)

class Failed(status: Status, val throwable: Throwable, val manualFlag: Boolean = false) : Status(status)

class Succeed(status: Status) : Status(status) {
    constructor(totalSize: Long) : this(Status(false, totalSize, totalSize))
}

