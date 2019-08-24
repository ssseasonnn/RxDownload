package zlc.season.rxdownload4.storage

import android.content.Context.MODE_PRIVATE
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.task.Task

class SimpleStorage : MemoryStorage() {
    private val sp by lazy {
        clarityPotion.getSharedPreferences("rxdownload_simple_storage", MODE_PRIVATE)
    }

    @Synchronized
    override fun load(task: Task) {
        super.load(task)

        if (isEmpty(task)) {
            localLoad(task)
            super.save(task)
        }
    }

    @Synchronized
    override fun save(task: Task) {
        super.save(task)
        localSave(task)
    }

    @Synchronized
    override fun delete(task: Task) {
        super.delete(task)
        localDelete(task)
    }

    private fun isEmpty(task: Task) = task.saveName.isEmpty() || task.savePath.isEmpty()

    private fun localSave(task: Task) {
        val key = task.hashCode().toString()
        val value = task.saveName + "\n" + task.savePath

        val editor = sp.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun localLoad(task: Task) {
        val key = task.hashCode().toString()
        val value = sp.getString(key, "")

        if (!value.isNullOrEmpty()) {
            val splits = value.split("\n")
            if (splits.size == 2) {
                task.saveName = splits[0]
                task.savePath = splits[1]
            } else {
                throw IllegalStateException("Task load failed!")
            }
        } else {
            throw IllegalStateException("Task load failed!")
        }
    }

    private fun localDelete(task: Task) {
        val key = task.hashCode().toString()
        sp.edit().remove(key).apply()
    }
}