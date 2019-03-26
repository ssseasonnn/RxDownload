package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


interface RetrofitApi {

    @HEAD
    fun checkByHead(@HeaderMap headers: Map<String, String>,
                    @Url url: String): Maybe<Response<Void>>

    @GET
    fun checkByGet(@HeaderMap headers: Map<String, String>,
                   @Url url: String): Maybe<Response<Void>>

    @GET
    @Streaming
    fun download(@HeaderMap headers: Map<String, String>,
                 @Url url: String): Maybe<Response<ResponseBody>>
}