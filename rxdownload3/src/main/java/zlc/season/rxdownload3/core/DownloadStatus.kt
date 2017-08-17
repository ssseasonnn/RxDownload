package zlc.season.rxdownload3.core


data class DownloadStatus(
        val status: Int = 0,
        val isChunked: Boolean = false,
        val downloadSize: Long = 0L,
        val totalSize: Long = 0L,
        val throwable: Throwable? = null
) {

    constructor(status: Int) : this() {

    }


}