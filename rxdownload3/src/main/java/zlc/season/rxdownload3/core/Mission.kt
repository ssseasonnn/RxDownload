package zlc.season.rxdownload3.core


interface Mission {
    fun tag(): String
    fun url(): String
    fun fileName(): String
    fun savePath(): String
}