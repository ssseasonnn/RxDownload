package zlc.season.rxdownload4

import android.os.Environment

val DEFAULT_SAVE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

val DEFAULT_RANGE_SIZE = 5 * 1024 * 1024 //5M

val RANGE_CHECK_HEADER = mapOf("Range" to "bytes=0-")
