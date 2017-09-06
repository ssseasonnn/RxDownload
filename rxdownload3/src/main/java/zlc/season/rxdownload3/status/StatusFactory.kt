package zlc.season.rxdownload3.status


interface StatusFactory {
    fun waiting(): Status

    fun downloading(isChunked: Boolean, downloadSize: Long, totalSize: Long): Status

    fun failed(throwable: Throwable): Status

    fun succeed(): Status
}