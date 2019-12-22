package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.Progress

fun Status.isEndStatus(): Boolean {
    return when (this) {
        is Normal -> false
        is Pending -> false
        is Started -> false
        is Downloading -> false
        is Paused -> true
        is Completed -> true
        is Failed -> true
        is Deleted -> true
    }
}

sealed class Status {
    var progress: Progress = Progress()
}

class Normal : Status()

class Pending : Status()

class Started : Status()

class Downloading : Status()

class Paused : Status()

class Completed : Status()

class Failed : Status() {
    var throwable: Throwable = RuntimeException("UNKNOWN ERROR")
}

class Deleted : Status()