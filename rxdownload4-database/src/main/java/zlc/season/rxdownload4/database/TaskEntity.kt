package zlc.season.rxdownload4.database

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.manager.*

@Entity(tableName = "task_record")
class TaskEntity(
        @PrimaryKey
        val id: Int,
        val url: String,
        val taskName: String,
        val saveName: String,
        val savePath: String,
        val status: Int = NORMAL,
        val downloadSize: Long = 0L,
        val totalSize: Long = 0L,
        val isChunked: Boolean = false,
        val time: Long = System.currentTimeMillis()
) {
    companion object {
        const val NORMAL = 0
        const val STARTED = 1
        const val DOWNLOADING = 2
        const val PAUSED = 3
        const val COMPLETED = 4
        const val FAILED = 5
        const val DELETED = 6

        fun mapStatus(status: Status): Int {
            return when (status) {
                is Normal -> NORMAL
                is Started -> STARTED
                is Downloading -> DOWNLOADING
                is Paused -> PAUSED
                is Completed -> COMPLETED
                is Failed -> FAILED
                is Deleted -> DELETED
            }
        }

        fun mapStatus(int: Int, downloadSize: Long, totalSize: Long, isChunked: Boolean): Status {
            val progress = Progress(downloadSize, totalSize, isChunked)
            val status = when (int) {
                NORMAL -> Normal()
                STARTED -> Started()
                DOWNLOADING -> Downloading()
                PAUSED -> Paused()
                COMPLETED -> Completed()
                FAILED -> Failed()
                DELETED -> Deleted()
                else -> throw IllegalStateException("UNKNOWN STATE")
            }
            status.progress = progress
            return status
        }
    }
}