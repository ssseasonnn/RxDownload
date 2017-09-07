package zlc.season.rxdownload3.status


class StatusFactoryImpl : StatusFactory {
    override fun waiting(): Status {
        return Waiting()
    }

    override fun downloading(chunkFlag: Boolean, downloadSize: Long, totalSize: Long): Status {
        return Downloading(chunkFlag, downloadSize, totalSize)
    }

    override fun failed(throwable: Throwable, manualFlag: Boolean): Status {
        return Failed(throwable, manualFlag)
    }

    override fun succeed(): Status {
        return Succeed()
    }
}