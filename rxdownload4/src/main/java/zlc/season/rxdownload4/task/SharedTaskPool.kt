package zlc.season.rxdownload4.task

object SharedTaskPool {
    private val map = mutableMapOf<Task, SharedTask>()

    @Synchronized
    fun add(task: Task, taskInfo: SharedTask) {
        map[task] = taskInfo
    }

    @Synchronized
    fun get(task: Task): SharedTask? {
        return map[task]
    }

    @Synchronized
    fun remove(task: Task) {
        map.remove(task)
    }
}