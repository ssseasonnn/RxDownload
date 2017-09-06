package zlc.season.rxdownload3.core


data class DownloadStatus(
        var isChunked: Boolean = false,
        var downloadSize: Long = 0L,
        var totalSize: Long = 0L
) {

    constructor(status: Int) : this() {

    }


}