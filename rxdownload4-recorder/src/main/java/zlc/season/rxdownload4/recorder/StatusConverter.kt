package zlc.season.rxdownload4.recorder

import androidx.room.TypeConverter
import zlc.season.rxdownload4.manager.*

class StatusConverter {
    companion object {
        const val NORMAL = 0  //never save
        const val STARTED = 1
        const val DOWNLOADING = 2
        const val PAUSED = 3
        const val COMPLETED = 4
        const val FAILED = 5
        const val DELETED = 6 //never save
        const val PENDING = 7 //never save
    }

    @TypeConverter
    fun intToStatus(status: Int): Status {
        return when (status) {
            NORMAL -> Normal()
            PENDING -> Pending()
            STARTED -> Started()
            DOWNLOADING -> Downloading()
            PAUSED -> Paused()
            COMPLETED -> Completed()
            FAILED -> Failed()
            DELETED -> Deleted()
            else -> throw IllegalStateException("UNKNOWN STATE")
        }
    }

    @TypeConverter
    fun statusToInt(status: Status): Int {
        return when (status) {
            is Normal -> NORMAL
            is Pending -> PENDING
            is Started -> STARTED
            is Downloading -> DOWNLOADING
            is Paused -> PAUSED
            is Completed -> COMPLETED
            is Failed -> FAILED
            is Deleted -> DELETED
        }
    }
}