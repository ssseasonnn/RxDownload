package zlc.season.rxdownload4.task

object TaskPool {
    private val map = mutableMapOf<String, Task>()

    @Synchronized
    fun add(task: Task) {
        map[task.tag()] = task
    }

    @Synchronized
    fun get(task: Task): Task {
        val result = map[task.tag()]
        if (result == null) {
            throw IllegalStateException("Task is null")
        } else {
            return result
        }
    }

    @Synchronized
    fun get(tag: String): Task {
        val result = map[tag]
        if (result == null) {
            throw IllegalStateException("Task is null")
        } else {
            return result
        }
    }

    @Synchronized
    fun isContain(task: Task): Boolean {
        return map.containsKey(task.tag())
    }

    @Synchronized
    fun isContain(tag: String): Boolean {
        return map.containsKey(tag)
    }

    @Synchronized
    fun remove(task: Task) {
        map.remove(task.tag())
    }

    @Synchronized
    fun remove(tag: String) {
        map.remove(tag)
    }
}