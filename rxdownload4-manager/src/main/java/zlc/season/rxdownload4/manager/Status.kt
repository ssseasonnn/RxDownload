package zlc.season.rxdownload4.manager

import zlc.season.rxdownload4.Progress

sealed class Status

class Normal : Status()

class Started : Status()

class Downloading : Status() {
    var progress: Progress = Progress()
        internal set
}

class Paused : Status()

class Completed : Status()

class Failed : Status() {
    var throwable: Throwable = RuntimeException("UNKNOWN ERROR")
        internal set
}