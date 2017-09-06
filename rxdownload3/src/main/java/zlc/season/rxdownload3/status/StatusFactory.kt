package zlc.season.rxdownload3.status


interface StatusFactory<out T : Status> {
    fun waitingStatus(): T

    fun downloadingStatus(): T

    fun failedStatus(): T

    fun succeedStatus(): T
}