package zlc.season.rxdownload3.core


interface Mission {
    fun provideTag(): String
    fun provideUrl(): String
    fun provideFileName(): String
    fun provideSavePath(): String
}