package zlc.season.rxdownload3.core


interface DownloadPlugin {
    fun isMissionExists(mission: Mission): Boolean
}