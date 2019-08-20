package zlc.season.rxdownload4.storage

import android.content.Context.MODE_PRIVATE
import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.log

class SimpleStorage : Storage {
    companion object {
        //memory cache
        private val taskPool = mutableMapOf<Int, Task>()
    }

    private val sp by lazy {
        clarityPotion.getSharedPreferences("rxdownload_simple_storage", MODE_PRIVATE)
    }

    @Synchronized
    override fun load(task: Task) {
        val result = taskPool[task.hashCode()]
        if (result == null) {
            localLoad(task)
            taskPool[task.hashCode()] = task
        } else {
            task.saveName = result.saveName
            task.savePath = result.savePath
        }
    }

    @Synchronized
    override fun save(task: Task) {
        taskPool[task.hashCode()] = task
        localSave(task)
    }

    @Synchronized
    override fun delete(task: Task) {
        taskPool.remove(task.hashCode())
        localDelete(task)
    }

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
                "task load failed".log()
            }
        } else {
            "task load failed".log()
        }
    }

    private fun localDelete(task: Task) {
        val key = task.hashCode().toString()
        sp.edit().remove(key).apply()
    }
}