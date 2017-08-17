package zlc.season.rxdownload3.core

import zlc.season.rxdownload3.helper.Crypto
import java.io.RandomAccessFile


class TmpFile(fileSaveName: String) {
    //________________________________________
    //|            |            |             |
    //|   Thread   | LastModify |     Byte    |
    //|____________|____________|_____________|
    //|            |            |             |
    //|     3      |  1231123   |    1 + 8    |
    //|____________|____________|_____________|
    //|_______________________________________|
    //|            |            |             |
    //|  start     |   end      |             |
    //|____________|____________|_____________|
    //|            |            |             |
    //|    0       |  10000     |      16     |
    //|____________|____________|_____________|
    //|            |            |             |
    //|  10001     |  20000     |      16     |
    //|____________|____________|_____________|
    //|            |            |             |
    //|  20001     |  30000     |      16     |
    //|____________|____________|_____________|
    //|_______________________________________|
    //|                         |             |
    //|   asdlfkjalsjdflasdfa   |     MD5     |
    //|_________________________|_____________|


    private val MODE = "rw"

    val thread: Byte = 3
    val lastModify: Long = 0L

    val tmpFileName = Crypto.md5(fileSaveName)

    lateinit var realFile: RandomAccessFile


    fun create() {
        realFile = RandomAccessFile(tmpFileName, MODE)
    }

    fun open() {

    }
}