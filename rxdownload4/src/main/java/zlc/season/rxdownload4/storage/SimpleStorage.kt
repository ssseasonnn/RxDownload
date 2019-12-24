package zlc.season.rxdownload4.storage

import android.content.Context.MODE_PRIVATE
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.task.Task

object SimpleStorage : MemoryStorage() {
    private val sp by lazy {
        clarityPotion.getSharedPreferences("rxdownload_simple_storage", MODE_PRIVATE)
    }

    @Synchronized
    override fun load(task: Task) {
        super.load(task)

        if (task.isEmpty()) {
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

    private fun localSave(task: Task) {
        val key = task.hashCode().toString()
        val value = task.taskName + "\n" + task.saveName + "\n" + task.savePath

        val editor = sp.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun localLoad(task: Task) {
        val key = task.hashCode().toString()
        val value = sp.getString(key, "")

        if (!value.isNullOrEmpty()) {
            val splits = value.split("\n")
            if (splits.size == 3) {
                task.taskName = splits[0]
                task.saveName = splits[1]
                task.savePath = splits[2]
            }
        }
    }

    private fun localDelete(task: Task) {
        val key = task.hashCode().toString()
        sp.edit().remove(key).apply()
    }
}