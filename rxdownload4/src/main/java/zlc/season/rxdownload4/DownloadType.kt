package zlc.season.rxdownload4

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response

interface DownloadType {
    fun download(response: Response<ResponseBody>): Flowable<Status>
}