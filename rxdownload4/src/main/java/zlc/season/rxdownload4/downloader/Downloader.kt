package zlc.season.rxdownload4.downloader

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.Status

interface Downloader {
    fun download(response: Response<ResponseBody>): Flowable<Status>
}