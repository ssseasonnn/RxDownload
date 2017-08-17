package zlc.season.rxdownload3.core


data class DownloadMission(
        val url: String,
        val fileName: String = "",
        val savePath: String = "") : Mission {


    override fun provideTag(): String {
        return url
    }

    override fun provideUrl(): String {
        return url
    }

    override fun provideFileName(): String {
        return fileName
    }

    override fun provideSavePath(): String {
        return savePath
    }
}