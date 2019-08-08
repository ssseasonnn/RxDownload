package zlc.season.rxdownload4.task

object TaskPool {
    private val map = mutableMapOf<Task, TaskInfo>()

    @Synchronized
    fun add(task: Task, taskInfo: TaskInfo): TaskInfo {
        map[task] = taskInfo
        return taskInfo
    }

    @Synchronized
    fun get(task: Task): TaskInfo? {
        return map[task]
    }
}