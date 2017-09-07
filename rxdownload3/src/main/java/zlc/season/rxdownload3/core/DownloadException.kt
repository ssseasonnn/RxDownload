package zlc.season.rxdownload3.core


class IllegalUrlException(message: String) : RuntimeException(message)

class MissionFailedException(cause: Throwable? = null) : RuntimeException("Mission stopped", cause)


