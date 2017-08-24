package zlc.season.rxdownload3.helper

import retrofit2.Response
import java.util.regex.Pattern


class ResponseUtil {
    companion object {

        fun isChunked(response: Response<*>): Boolean {
            return "chunked" == transferEncoding(response)
        }

        fun isSupportRange(resp: Response<*>): Boolean {
            if (!resp.isSuccessful) {
                return false
            }

            if (contentRange(resp).isNullOrEmpty() || acceptRanges(resp).isNullOrEmpty()) {
                return false
            }

            return true
        }

        fun fileName(url: String): String {
            return url.substring(url.lastIndexOf('/') + 1)
        }

        fun contentDisposition(response: Response<*>): String {
            val disposition = response.headers().get("Content-Disposition")

            if (disposition == null || disposition.isEmpty()) {
                return ""
            }

            val matcher = Pattern.compile(".*filename=(.*)").matcher(disposition.toLowerCase())
            if (!matcher.find()) {
                return ""
            }

            var result = matcher.group(1)
            if (result.startsWith("\"")) {
                result = result.substring(1)
            }
            if (result.endsWith("\"")) {
                result = result.substring(0, result.length - 1)
            }
            return result
        }


        private fun transferEncoding(response: Response<*>): String? {
            return response.headers().get("Transfer-Encoding")
        }

        private fun contentRange(response: Response<*>): String? {
            return response.headers().get("Content-Range")
        }

        private fun acceptRanges(response: Response<*>): String? {
            return response.headers().get("Accept-Ranges")
        }
    }
}