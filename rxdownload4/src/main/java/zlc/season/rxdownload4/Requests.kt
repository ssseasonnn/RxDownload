package zlc.season.rxdownload4

import io.reactivex.Flowable
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface Requests {

    @GET
    @Streaming
    fun download(
            @Url url: String,
            @HeaderMap headers: Map<String, String> = emptyMap()
    ): Flowable<Response<ResponseBody>>

    companion object {
        private const val FAKE_BASE_URL = "http://www.example.com"

        private val builder = OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)

        private val client = builder.build()

        fun get(baseUrl: String = FAKE_BASE_URL): Requests {
            val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            return retrofit.create(Requests::class.java)
        }
    }
}