package zlc.season.rxdownload3.core

open class Mission(val url: String,
                   var saveName: String = "",
                   var savePath: String = "",
                   var rangeFlag: Boolean? = null,
                   var tag: String = url) {

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