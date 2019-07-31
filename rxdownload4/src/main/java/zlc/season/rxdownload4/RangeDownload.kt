package zlc.season.rxdownload4

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

class RangeDownload(private val file: File) : DownloadType {
    private val shadowFile = file.shadow()

    override fun download(response: Response<ResponseBody>): Flowable<Status> {

    }
}