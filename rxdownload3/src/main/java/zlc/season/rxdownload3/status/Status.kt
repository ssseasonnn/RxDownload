package zlc.season.rxdownload3.status


abstract class Status(val status: Int) {

    companion object STATUS {
        val WAITING = 10
        val DOWNLOADING = 11
        val FAILED = 12
        val SUCCEED = 13
    }
}

class Waiting : Status(WAITING)

class Downloading(var chunkFlag: Boolean = false,
                  var downloadSize: Long = 0L,
                  var totalSize: Long = 0L) : Status(DOWNLOADING)

class Failed(val throwable: Throwable, val manualFlag: Boolean) : Status(FAILED)

class Succeed : Status(SUCCEED)

