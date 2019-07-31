package zlc.season.rxdownload4

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.utils.contentLength
import zlc.season.rxdownload4.utils.file
import zlc.season.rxdownload4.utils.shadow
import zlc.season.rxdownload4.utils.tmp

class RangeDownloader : Downloader {

    override fun download(response: Response<ResponseBody>): Flowable<Status> {
        val totalSize = response.contentLength()

        prepare(response)

    }

    private fun prepare(response: Response<ResponseBody>) {
        val file = response.file()
        val shadowFile = file.shadow()
        val tmpFile = file.tmp()

        if (file.exists() && file.isFile) {
            if (shadowFile.exists() || tmpFile.exists()) {
                shadowFile.deleteOnExit()
                tmpFile.deleteOnExit()
            }

        } else {
            //both necessary file exits
            if (shadowFile.exists() && tmpFile.exists()) {
                //check tmp file valid
                //continue
            } else {
                tmpFile.deleteOnExit()
                shadowFile.deleteOnExit()

                val tmpCreated = tmpFile.createNewFile()
                val shadowCreated = shadowFile.createNewFile()

                if (tmpCreated && shadowCreated) {
                    //begin
                }
            }
        }
    }
}