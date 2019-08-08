package zlc.season.rxdownload4.utils

import io.reactivex.disposables.Disposable
import java.io.Closeable
import java.text.DecimalFormat

fun Disposable?.safeDispose() {
    if (this != null && !isDisposed) {
        dispose()
    }
}

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

fun formatSize(size: Long): String {
    if (size < 0) {
        throw IllegalArgumentException("Size must larger than 0.")
    }

    val b = size.toDouble()
    val k = size / 1024.0
    val m = size / 1024.0 / 1024.0
    val g = size / 1024.0 / 1024.0 / 1024.0
    val t = size / 1024.0 / 1024.0 / 1024.0 / 1024.0
    val dec = DecimalFormat("0.00")

    return when {
        t > 1 -> dec.format(t) + " TB"
        g > 1 -> dec.format(g) + " GB"
        m > 1 -> dec.format(m) + " MB"
        k > 1 -> dec.format(k) + " KB"
        else -> dec.format(b) + " B"
    }
}