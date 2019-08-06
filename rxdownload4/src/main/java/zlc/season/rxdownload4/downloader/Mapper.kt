package zlc.season.rxdownload4.downloader

import retrofit2.Response

interface Mapper {
    fun map(response: Response<*>): Downloader
}