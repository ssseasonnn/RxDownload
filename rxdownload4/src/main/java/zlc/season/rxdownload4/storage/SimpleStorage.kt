package zlc.season.rxdownload4.storage

import zlc.season.claritypotion.ClarityPotion.Companion.clarityPotion
import zlc.season.rxdownload4.task.Task

class SimpleStorage : Storage {
    override fun load(task: Task) {
        clarityPotion
    }

    override fun save(task: Task) {

    }
}