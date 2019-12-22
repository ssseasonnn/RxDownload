package zlc.season.rxdownload4.manager

interface TaskLimitation {
    fun start(taskManager: TaskManager)

    fun stop(taskManager: TaskManager)

    fun delete(taskManager: TaskManager)
}