package zlc.season.rxdownload4.utils

import java.io.Closeable

fun Closeable.safeClose() {
    try {
        close()
    } catch (ignore: Throwable) {

    }
}