package zlc.season.rxdownload4.downloader

import okhttp3.ResponseBody
import retrofit2.Response

interface Dispatcher {
    fun dispatch(response: Response<ResponseBody>): Downloader
}