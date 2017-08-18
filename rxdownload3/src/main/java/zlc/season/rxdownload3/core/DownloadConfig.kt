package zlc.season.rxdownload3.core

import android.os.Environment


object DownloadConfig {
    var defaultSavePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    var retryCount = 3
    var threadCount = 3
    var perSize: Int = 5 * 1024  //KB

}