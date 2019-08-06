package zlc.season.rxdownload4.request

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Streaming
import retrofit2.http.Url
import zlc.season.rxdownload4.RANGE_CHECK_HEADER

interface Request {

    @GET
    @Streaming
    fun get(
            @Url url: String,
            @HeaderMap headers: Map<String, String> = RANGE_CHECK_HEADER
    ): Flowable<Response<ResponseBody>>

    companion object {
        private val request = request<Request>()

        operator fun invoke(): Request {
            return request
        }
    }
}
