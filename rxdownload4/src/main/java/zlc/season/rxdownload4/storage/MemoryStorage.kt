package zlc.season.rxdownload4.storage

import zlc.season.rxdownload4.task.Task

open class MemoryStorage : Storage {
    companion object {
        //memory cache
        private val taskPool = mutableMapOf<Task, Task>()
    }

    @Synchronized
    override fun load(task: Task) {
        val result = taskPool[task]
        if (result != null) {
            task.saveName = result.saveName
            task.savePath = result.savePath
        }
    }

    @Synchronized
    override fun save(task: Task) {
        taskPool[task] = task
    }

    @Synchronized
    override fun delete(task: Task) {
        taskPool.remove(task)
    }
}