package zlc.season.rxdownload4.downloader

import retrofit2.Response
import zlc.season.rxdownload4.utils.isSupportRange

class DefaultMapper : Mapper {
    override fun map(response: Response<*>): Downloader {
        if (!response.isSuccessful) {
            throw IllegalStateException("Response is failed!")
        }

        return if (response.isSupportRange()) {
            RangeDownloader()
        } else {
            NormalDownloader()
        }
    }
}