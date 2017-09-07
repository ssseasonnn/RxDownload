package zlc.season.rxdownload3.status


interface StatusFactory {
    fun waiting(): Status

    fun downloading(chunkFlag: Boolean, downloadSize: Long, totalSize: Long): Status

    fun failed(throwable: Throwable, manualFlag: Boolean): Status

    fun succeed(): Status
}