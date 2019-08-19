package zlc.season.rxdownload4.management

import zlc.season.rxdownload4.task.Task

object SharedTaskPool {
    private val map = mutableMapOf<Task, SharedTask>()

    @Synchronized
    fun add(task: Task, taskInfo: SharedTask): SharedTask {
        map[task] = taskInfo
        return taskInfo
    }

    @Synchronized
    fun get(task: Task): SharedTask? {
        return map[task]
    }
}