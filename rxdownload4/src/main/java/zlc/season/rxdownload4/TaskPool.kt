package zlc.season.rxdownload4

object TaskPool {
    private val map = mutableMapOf<String, Task>()

    @Synchronized
    fun add(task: Task) {
        if (map[task.url] == null) {
            map[task.url] = task
        }
    }
}