package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url


interface RetrofitApi {

    @GET
    fun check(@Header("Range") range: String,
              @Header("If-Modified-Since") lastModify: String,
              @Url url: String): Maybe<Response<Void>>

}