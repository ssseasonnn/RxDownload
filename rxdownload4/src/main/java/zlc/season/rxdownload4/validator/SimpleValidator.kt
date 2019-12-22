package zlc.season.rxdownload4.validator

import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.utils.contentLength
import java.io.File

object SimpleValidator : Validator {
    override fun validate(file: File, response: Response<ResponseBody>): Boolean {
        return file.length() == response.contentLength()
    }
}