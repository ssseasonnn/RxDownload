package zlc.season.rxdownload3.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import zlc.season.rxdownload3.database.DbActor
import zlc.season.rxdownload3.database.SQLiteActor


@SuppressLint("StaticFieldLeak")
object DownloadConfig {
    val DEBUG = true

    val ANY = Any()

    val DOWNLOADING_FILE_SUFFIX = ".download"
    val TMP_DIR_SUFFIX = ".TMP"
    val TMP_FILE_SUFFIX = ".tmp"

    val RANGE_DOWNLOAD_SIZE: Long = 5 * 1024 * 1024  // 5M

    var maxConcurrencyMission = 3
    var maxConcurrencyRange = 3

    var defaultSavePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path

    lateinit var applicationContext: Context

    lateinit var DB: DbActor

    fun init(context: Context) {
        applicationContext = context.applicationContext
        DB = SQLiteActor(context.applicationContext)
    }
}