package zlc.season.rxdownload3.status


class DownloadStatus : Status {
    var isChunked: Boolean = false
    var downloadSize: Long = 0L
    var totalSize: Long = 0L

    var status: Int = 0
    val throwable: Throwable? = null

    companion object STATUS {
        val WAITING = 10
        val DOWNLOADING = 11
        val FAILED = 12
        val SUCCEED = 13
    }
}