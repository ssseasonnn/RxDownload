package zlc.season.rxdownload4.request

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Streaming
import retrofit2.http.Url

object RequestImpl : Request {
    private val api = request<Api>()

    override fun get(url: String, headers: Map<String, String>): Flowable<Response<ResponseBody>> {
        return api.get(url, headers)
    }

    interface Api {
        @GET
        @Streaming
        fun get(
                @Url url: String,
                @HeaderMap headers: Map<String, String>
        ): Flowable<Response<ResponseBody>>
    }
}

