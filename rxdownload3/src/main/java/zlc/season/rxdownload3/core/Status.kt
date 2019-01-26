package zlc.season.rxdownload3.core

import zlc.season.rxdownload3.helper.formatSize
import java.text.NumberFormat.getPercentInstance


open class Status(var downloadSize: Long = 0L,
                  var totalSize: Long = 0L,
                  var chunkFlag: Boolean = false) {

    constructor(status: Status) : this(status.downloadSize, status.totalSize, status.chunkFlag)

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

class Normal(status: Status) : Status(status) {
    override fun toString(): String {
        return "Normal"
    }
}

class Suspend(status: Status) : Status(status){
    override fun toString(): String {
        return "Suspend"
    }
}

class Waiting(status: Status) : Status(status){
    override fun toString(): String {
        return "Waiting"
    }
}

class Downloading(status: Status) : Status(status){
    override fun toString(): String {
        return "Downloading: ${formatString()}"
    }
}

class Failed(status: Status, val throwable: Throwable) : Status(status){
    override fun toString(): String {
        return "Failed"
    }
}

class Succeed(status: Status) : Status(status){
    override fun toString(): String {
        return "Succeed"
    }
}

class Deleted(status: Status) : Status(status){
    override fun toString(): String {
        return "Deleted"
    }
}



