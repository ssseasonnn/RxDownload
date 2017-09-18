package zlc.season.rxdownload3.http

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OkHttpClientFactoryImpl : OkHttpClientFactory {
    override fun build(): OkHttpClient {
        val builder = OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
        return builder.build()
    }
}