package zlc.season.rxdownload4.utils

import io.reactivex.disposables.Disposable
import java.io.Closeable
import java.math.BigDecimal

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

fun Long.formatSize(): String {
    if (this < 0) {
        throw IllegalArgumentException("Size must larger than 0.")
    }

    val byte = this.toDouble()
    val kb = byte / 1024.0
    val mb = byte / 1024.0 / 1024.0
    val gb = byte / 1024.0 / 1024.0 / 1024.0
    val tb = byte / 1024.0 / 1024.0 / 1024.0 / 1024.0

    return when {
        tb >= 1 -> "${tb.decimal(2)} TB"
        gb >= 1 -> "${gb.decimal(2)} GB"
        mb >= 1 -> "${mb.decimal(2)} MB"
        kb >= 1 -> "${kb.decimal(2)} KB"
        else -> "${byte.decimal(2)} B"
    }
}

fun Double.decimal(digits: Int): Double {
    return this.toBigDecimal()
            .setScale(digits, BigDecimal.ROUND_HALF_UP)
            .toDouble()
}
