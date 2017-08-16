package zlc.season.rxdownload3.core

import zlc.season.rxdownload3.helper.Crypto
import java.io.RandomAccessFile


class TmpFile(fileSaveName: String) {
    private val MODE = "rw"

    val tmpFileName = Crypto.md5(fileSaveName)

    lateinit var realFile: RandomAccessFile



    fun create() {
        realFile = RandomAccessFile(tmpFileName, MODE)
    }

    fun open() {

    }
}