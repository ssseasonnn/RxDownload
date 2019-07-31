package zlc.season.rxdownload4

import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


const val FAKE_BASE_URL = "http://www.example.com"

val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .build()


inline fun <reified T> request(
        baseUrl: String = FAKE_BASE_URL,
        client: OkHttpClient = okHttpClient,
        callAdapterFactory: CallAdapter.Factory = RxJava2CallAdapterFactory.create(),
        converterFactory: Converter.Factory = GsonConverterFactory.create()
): T {
    val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(converterFactory)
            .build()

    return retrofit.create(T::class.java)
}