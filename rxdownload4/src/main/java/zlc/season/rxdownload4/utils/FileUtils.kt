package zlc.season.rxdownload4.utils

import java.io.File

fun checkFile(file: File): Boolean {
    if (file.exists()) {
        return true
    } else {
        return false
    }
}

fun File.createDir() {
    mkdirs()
}

fun File.shadow(): File {
    val shadowPath = "$canonicalPath.download"
    return File(shadowPath)
}

fun File.tmp(): File {
    val tmpPath = "$canonicalPath.tmp"
    return File(tmpPath)
}

fun File.recreate() {
    if (exists()) {
        delete()
    }
    createNewFile()
}