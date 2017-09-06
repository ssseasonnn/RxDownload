package zlc.season.rxdownload3.core

import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import zlc.season.rxdownload3.database.SqliteAdapter
import zlc.season.rxdownload3.status.StatusFactoryImpl


internal object DownloadConfig {
    var DEFAULT_SAVE_PATH = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path
    var MAX_MISSION_NUMBER = 3
    var MAX_CONCURRENCY = 3
    var RANGE_DOWNLOAD_SIZE: Long = 5 * 1024 * 1024 // 5M

    var DB = SqliteAdapter()
    var FACTORY = StatusFactoryImpl()
}