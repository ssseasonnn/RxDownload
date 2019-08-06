package zlc.season.rxdownload4

import android.os.Environment
import zlc.season.rxdownload4.downloader.DefaultMapper
import zlc.season.rxdownload4.validator.SimpleValidator

val DEFAULT_SAVE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

val DEFAULT_RANGE_SIZE = 5L * 1024 * 1024 //5M

val DEFAULT_MAX_CONCURRENCY = 3

val RANGE_CHECK_HEADER = mapOf("Range" to "bytes=0-")

val DEFAULT_VALIDATOR = SimpleValidator()

val DEFAULT_MAPPER = DefaultMapper()
