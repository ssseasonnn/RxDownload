package zlc.season.rxdownload3.status


class StatusFactoryImpl : StatusFactory {
    override fun waiting(): Status {
        return Waiting()
    }

    override fun downloading(isChunked: Boolean, downloadSize: Long, totalSize: Long): Status {
        return Downloading(isChunked, downloadSize, totalSize)
    }

    override fun failed(throwable: Throwable): Status {
        return Failed(throwable)
    }

    override fun succeed(): Status {
        return Succeed()
    }
}