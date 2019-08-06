package zlc.season.rxdownload4.downloader

import okhttp3.ResponseBody
import retrofit2.Response

interface Mapper {
    fun map(response: Response<ResponseBody>): Downloader
}