package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.task.Task

object TaskManagerPool {
    private val map = mutableMapOf<Task, TaskManager>()

    @Synchronized
    fun add(task: Task, taskManager: TaskManager) {
        map[task] = taskManager
    }

    @Synchronized
    fun get(task: Task): TaskManager? {
        return map[task]
    }

    @Synchronized
    fun remove(task: Task) {
        map.remove(task)
    }
}