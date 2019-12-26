package zlc.season.rxdownload4.manager

class BasicTaskLimitation(private val maxTaskNumber: Int) : TaskLimitation {
    private val taskManagerList = mutableListOf<TaskManager>()
    private var currentTakNumber = 0

    companion object {
        private const val MAX_TASK_NUMBER = 3

        @Volatile
        private var INSTANCE: BasicTaskLimitation? = null

        fun of(maxTaskNumber: Int = MAX_TASK_NUMBER): TaskLimitation =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: BasicTaskLimitation(maxTaskNumber).also {
                        INSTANCE = it
                    }
                }
    }

    override fun start(taskManager: TaskManager) {
        if (currentTakNumber < maxTaskNumber) {
            currentTakNumber++
            taskManager.innerStart()

            val tag = Any()
            taskManager.addCallback(tag, false) {
                if (it.isEndStatus()) {
                    taskManager.removeCallback(tag)
                    currentTakNumber--
                    if (taskManagerList.size > 0) {
                        val firstTaskManager = taskManagerList.removeAt(0)
                        start(firstTaskManager)
                    }
                }
            }

        } else {
            taskManagerList.add(taskManager)
            taskManager.innerPending()
        }
    }

    override fun stop(taskManager: TaskManager) {
        taskManagerList.remove(taskManager)
        taskManager.innerStop()
    }

    override fun delete(taskManager: TaskManager) {
        taskManagerList.remove(taskManager)
        taskManager.innerDelete()
    }
}