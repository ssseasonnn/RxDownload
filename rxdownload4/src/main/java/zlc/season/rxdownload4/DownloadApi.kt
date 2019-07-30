package zlc.season.rxdownload4

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadApi {

    @GET
    @Streaming
    fun download(
            @Url url: String,
            @HeaderMap headers: Map<String, String> = emptyMap()
    ): Flowable<Response<ResponseBody>>
}
