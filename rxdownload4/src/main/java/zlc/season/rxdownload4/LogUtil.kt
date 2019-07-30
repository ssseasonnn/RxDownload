package zlc.season.rxdownload4

import android.util.Log

const val LOG_ENABLE = true
const val LOG_TAG = "RxDownload"

fun Any.log() {
    if (LOG_ENABLE) {
        if (this is Throwable) {
            Log.w(LOG_TAG, this.message, this)
        } else {
            Log.d(LOG_TAG, toString())
        }
    }
}