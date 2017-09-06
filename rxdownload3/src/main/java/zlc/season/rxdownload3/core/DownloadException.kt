package zlc.season.rxdownload3.core


class IllegalUrlException(message: String) : RuntimeException(message)

class MissionStoppedException(cause: Throwable? = null) : RuntimeException("Mission stopped", cause)


