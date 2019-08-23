package zlc.season.rxdownload4.management

sealed class Status {

}

class Normal : Status()

class Started : Status()

class Paused : Status()

class Completed : Status()

class Failed : Status()