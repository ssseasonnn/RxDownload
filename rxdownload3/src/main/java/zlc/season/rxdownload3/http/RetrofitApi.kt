package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url


interface RetrofitApi {

    @GET
    fun check(@Header("Range") range: String,
              @Url url: String): Maybe<Response<Void>>

    @GET
    @Streaming
    fun download(@Header("Range") range: String?,
                 @Url url: String): Maybe<Response<ResponseBody>>
}