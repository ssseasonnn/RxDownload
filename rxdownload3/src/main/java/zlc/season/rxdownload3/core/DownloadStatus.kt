package zlc.season.rxdownload3.core


data class DownloadStatus(
        var status: Int = 0,
        var isChunked: Boolean = false,
        var downloadSize: Long = 0L,
        var totalSize: Long = 0L,
        var throwable: Throwable? = null
) {

    constructor(status: Int) : this() {

    }


}