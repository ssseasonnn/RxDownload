package zlc.season.rxdownload3.core


interface Mission {
    fun providerTag(): String
    fun provideUrl(): String
    fun provideFileName(): String
    fun provideSavePath(): String
}