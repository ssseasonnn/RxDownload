package zlc.season.rxdownload3.core


data class DownloadStatus(
        val isChunked: Boolean,
        val downloadSize: Long,
        val totalSize: Long,
        val throwable: Throwable
)