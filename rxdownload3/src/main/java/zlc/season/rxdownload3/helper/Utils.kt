package zlc.season.rxdownload3.helper

import android.content.Context
import android.content.pm.PackageManager
import io.reactivex.disposables.Disposable
import java.io.File
import java.text.DecimalFormat

internal val ANY = Any()

fun dispose(disposable: Disposable?) {
    if (disposable != null && !disposable.isDisposed) {
        disposable.dispose()
    }
}

fun formatSize(size: Long): String {
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

fun getPackageName(context: Context, apkFile: File): String {
    val pm = context.packageManager
    val apkInfo = pm.getPackageArchiveInfo(apkFile.path, PackageManager.GET_META_DATA)
    return apkInfo.packageName
}