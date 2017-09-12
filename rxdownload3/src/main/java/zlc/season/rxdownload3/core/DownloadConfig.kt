package zlc.season.rxdownload3.core

import android.content.Context
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import zlc.season.rxdownload3.database.DbActor
import zlc.season.rxdownload3.database.SqliteActor


object DownloadConfig {
    var DEBUG = true

    val ANY = Any()
    val DOWNLOADING_FILE_SUFFIX = ".download"
    val TMP_DIR_SUFFIX = ".TMP"
    val TMP_FILE_SUFFIX = ".tmp"


    /**
     * Every range download size.
     */
    val RANGE_DOWNLOAD_SIZE: Long = 5 * 1024 * 1024  // 5M

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

    lateinit var DB :DbActor

    fun init(context: Context) {
        DB = SqliteActor(context.applicationContext)
    }
}