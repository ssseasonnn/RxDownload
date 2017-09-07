package zlc.season.rxdownload3.core


class Mission(val url: String) {
    var tag: String = url

    var fileName: String = ""
    var savePath: String = ""

    var forceReDownload: Boolean = false

    var rangeFlag: Boolean? = null

    var currentSize: Long = 0L
    var totalSize: Long = 0L

    constructor(
            url: String,
            fileName: String = "",
            savePath: String = "",
            tag: String = "",
            forceReDownload: Boolean = false
    ) : this(url) {
        this.fileName = fileName
        this.savePath = savePath
        this.tag = tag
        this.forceReDownload = forceReDownload
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mission

        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        return tag.hashCode()
    }
}