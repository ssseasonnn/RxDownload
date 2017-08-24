package zlc.season.rxdownload3.core

import zlc.season.rxdownload3.helper.Crypto
import java.io.File
import java.io.RandomAccessFile


class TmpFile(missionWrapper: MissionWrapper) : DownloadFile(missionWrapper) {
    private val TMP_DIR_SUFFIX = ".TMP"
    private val TMP_FILE_SUFFIX = ".tmp"
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


    lateinit var file: RandomAccessFile

    init {
//        val tmpDirPath = path + File.separator + TMP_DIR_SUFFIX
//        val tmpFilePath = tmpDirPath + File.separator + saveName + TMP_FILE_SUFFIX
//        file = RandomAccessFile(File(tmpFilePath), MODE)
    }

    fun create() {
        
    }

    fun open() {

    }
}