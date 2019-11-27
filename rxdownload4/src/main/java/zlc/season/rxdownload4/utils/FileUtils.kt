package zlc.season.rxdownload4.utils

import zlc.season.rxdownload4.task.Task
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

fun File.shadow(): File {
    val shadowPath = "$canonicalPath.download"
    return File(shadowPath)
}

fun File.tmp(): File {
    val tmpPath = "$canonicalPath.tmp"
    return File(tmpPath)
}

fun File.recreate(block: () -> Unit = {}) {
    delete()
    val created = createNewFile()
    if (created) {
        block()
    } else {
        throw IllegalStateException("File create failed!")
    }
}

fun File.channel(): FileChannel {
    return RandomAccessFile(this, "rw").channel
}

fun File.clear() {
    val shadow = shadow()
    val tmp = tmp()
    shadow.delete()
    tmp.delete()
    delete()
}

internal fun Task.getDir(): File {
    return File(savePath)
}

internal fun Task.getFile(): File {
    return File(savePath, saveName)
}
