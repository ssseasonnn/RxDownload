package zlc.season.rxdownload4

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.utils.contentLength
import zlc.season.rxdownload4.utils.file
import zlc.season.rxdownload4.utils.shadow
import zlc.season.rxdownload4.utils.tmp
import java.io.File

class RangeDownloader : Downloader {
    lateinit var rangeTmpFile: RangeTmpFile

    override fun download(response: Response<ResponseBody>): Flowable<Status> {
        val totalSize = response.contentLength()

        prepare(response)

    }

    private fun prepare(response: Response<ResponseBody>) {
        val file = response.file()
        val shadowFile = file.shadow()
        val tmpFile = file.tmp()

        if (file.exists()) {

        } else {
            val totalSize = response.contentLength()

            if (shadowFile.exists() && tmpFile.exists()) {
                rangeTmpFile = RangeTmpFile(tmpFile, totalSize)
                rangeTmpFile.read()
                if (rangeTmpFile.check()) {

                } else {
                    recreate(tmpFile, shadowFile, totalSize)
                }
            } else {
                recreate(tmpFile, shadowFile, totalSize)
            }
        }
    }

    private fun recreate(
            tmpFile: File,
            shadowFile: File,
            totalSize: Long
    ) {
        tmpFile.deleteOnExit()
        shadowFile.deleteOnExit()

        val tmpCreated = tmpFile.createNewFile()
        val shadowCreated = shadowFile.createNewFile()

        if (tmpCreated && shadowCreated) {
            //begin
            rangeTmpFile = RangeTmpFile(tmpFile, totalSize)
            rangeTmpFile.write()
        }
    }
}