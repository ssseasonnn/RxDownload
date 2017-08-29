package zlc.season.rxdownload3.core

import android.os.Environment


object DownloadConfig {
    var DEFAULT_SAVE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    var retryCount = 3
    var MAX_CONCURRENCY = 3
    var RANGE_DOWNLOAD_SIZE: Long = 5 * 1024 * 1024 // 5M

}