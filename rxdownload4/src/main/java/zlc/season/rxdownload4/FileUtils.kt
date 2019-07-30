package zlc.season.rxdownload4

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
    val path = canonicalPath
    val shadowPath = "$path.download"
    return File(shadowPath)
}

fun File.recreate() {
    if (exists()) {
        delete()
    }
    createNewFile()
}