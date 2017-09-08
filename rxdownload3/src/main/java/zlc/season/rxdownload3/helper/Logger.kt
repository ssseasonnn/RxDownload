package zlc.season.rxdownload3.helper

import android.util.Log
import zlc.season.rxdownload3.core.DownloadConfig.DEBUG


private val TAG = "RxDownload"

fun logi(message: String) {
    if (DEBUG) {
        Log.i(TAG, message)
    }
}

fun loge(message: String, throwable: Throwable? = null) {
    if (DEBUG) {
        Log.e(TAG, message, throwable)
    }
}

fun logd(message: String) {
    if (DEBUG) {
        Log.d(TAG, message)
    }
}