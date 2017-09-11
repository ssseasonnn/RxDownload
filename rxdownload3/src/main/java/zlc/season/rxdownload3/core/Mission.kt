package zlc.season.rxdownload3.core


class Mission(val url: String) {
    var tag = url
    var saveName = ""
    var savePath = ""

    var rangeFlag: Boolean? = null

    constructor(
            url: String,
            fileName: String = "",
            savePath: String = "",
            tag: String = ""
    ) : this(url) {
        this.saveName = fileName
        this.savePath = savePath
        this.tag = tag
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