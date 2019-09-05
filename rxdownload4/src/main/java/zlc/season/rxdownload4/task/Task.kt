package zlc.season.rxdownload4.task

import zlc.season.rxdownload4.DEFAULT_SAVE_PATH
import zlc.season.rxdownload4.utils.getFileNameFromUrl

open class Task @JvmOverloads constructor(
        val url: String,
        taskName: String = getFileNameFromUrl(url),
        saveName: String = "",
        savePath: String = DEFAULT_SAVE_PATH
) {
    var taskName: String = taskName
        internal set

    var saveName: String = saveName
        internal set

    var savePath: String = savePath
        internal set

    /**
     * Each task with unique tag.
     */
    open fun tag() = url


    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true

        return if (other is Task) {
            tag() == other.tag()
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return tag().hashCode()
    }

    open fun isEmpty(): Boolean {
        return taskName.isEmpty() || saveName.isEmpty() || savePath.isEmpty()
    }
}