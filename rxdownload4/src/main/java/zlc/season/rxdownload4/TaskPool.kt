package zlc.season.rxdownload4

object TaskPool {
    private val map = mutableMapOf<String, Task>()

    @Synchronized
    fun add(task: Task) {
        if (map[task.url] == null) {
            map[task.url] = task
        }
    }

//    @Synchronized
//    fun get(task: Task): Task {
//    }

    @Synchronized
    fun isContain(task: Task): Boolean {
        return map[task.url] != null
    }

    @Synchronized
    fun remove(task: Task) {
        map.remove(task.url)
    }
}