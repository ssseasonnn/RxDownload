package zlc.season.rxdownload3.core


data class DownloadMission(
        val url: String,
        val fileName: String = "",
        val savePath: String = "") : Mission {


    override fun tag(): String {
        return url
    }

    override fun url(): String {
        return url
    }

    override fun fileName(): String {
        return fileName
    }

    override fun savePath(): String {
        return savePath
    }
}