package zlc.season.rxdownload3.core

import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import zlc.season.rxdownload3.database.EmptyDbAdapter


object DownloadConfig {
    val ANY = Any()

    /**
     * Every range download size.
     */
    val RANGE_DOWNLOAD_SIZE: Long = 5 * 1024 * 1024 // 5M

    /**
     * Max download mission number, default is 3 count
     */
    var MAX_MISSION_NUMBER = 3

    /**
     * Max concurrency each mission
     */
    var MAX_CONCURRENCY = 3

    /**
     * Default file save path, default is Downloads dir
     */
    var DEFAULT_SAVE_PATH = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path

    var DB = EmptyDbAdapter()
}