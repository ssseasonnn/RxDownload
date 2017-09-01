package zlc.season.rxdownload3.core


class IllegalUrlException(message: String) : RuntimeException(message)

class MissionExitsException : RuntimeException("Mission exists")

class MissionStoppedException : RuntimeException("Mission stopped")