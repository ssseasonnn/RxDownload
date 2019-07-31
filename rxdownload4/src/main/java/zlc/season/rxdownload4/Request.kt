package zlc.season.rxdownload4

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface Request {

    @HEAD
    @Streaming
    fun checkWithHead(
            @Url url: String,
            @HeaderMap headers: Map<String, String> = emptyMap()
    ): Flowable<Response<Void>>

    @GET
    @Streaming
    fun checkWithGet(
            @Url url: String,
            @HeaderMap headers: Map<String, String> = emptyMap()
    ): Flowable<Response<Void>>

    @GET
    @Streaming
    fun get(
            @Url url: String,
            @HeaderMap headers: Map<String, String> = emptyMap()
    ): Flowable<Response<ResponseBody>>
}
