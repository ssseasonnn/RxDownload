package zlc.season.rxdownload3.http

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import zlc.season.rxdownload3.core.DownloadConfig


object RetrofitClient {
    private val FAKE_BASE_URL = "http://www.example.com"
    private val okHttpClientFactory = DownloadConfig.okHttpClientFactory

    fun get(baseUrl: String = FAKE_BASE_URL): Retrofit {
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClientFactory.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
    }
}