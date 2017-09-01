package zlc.season.rxdownload3.core


class Mission(
        val url: String,
        val fileName: String = "",
        val savePath: String = "",
        val autoStart: Boolean = false) {

    val tag: String
        get() = url
}