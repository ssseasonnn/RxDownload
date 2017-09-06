package zlc.season.rxdownload3.core


class Mission(
        val url: String,
        val fileName: String = "",
        val savePath: String = "") {

    val tag: String
        get() = url

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