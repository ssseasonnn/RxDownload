package zlc.season.rxdownload4

import java.io.File

fun checkFile(file: File): Boolean {
    if (file.exists()) {
        return true
    } else {
        return false
    }
}

fun createDir(file: File) {
    file.mkdirs()
}

fun File.shadowFile(): File {
    val path = canonicalPath
    val shadowPath = "$path.download"
    return File(shadowPath)
}

fun recreateFile(path: String) {
    val file = File(path)
    if (file.exists()) {
        file.deleteRecursively()
    }
}