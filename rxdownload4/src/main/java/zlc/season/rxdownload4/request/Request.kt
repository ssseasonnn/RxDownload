package zlc.season.rxdownload4.request

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response

interface Request {
    fun get(url: String, headers: Map<String, String>): Flowable<Response<ResponseBody>>
}