package zlc.season.rxdownload4.utils

import java.io.Closeable

fun Closeable.safeClose() {
    try {
        close()
    } catch (ignore: Throwable) {

    }
}

fun String.toLongOrDefault(defaultValue: Long): Long {
    return try {
        toLong()
    } catch (_: NumberFormatException) {
        defaultValue
    }
}