package zlc.season.rxdownload3.helper

import okhttp3.internal.http.HttpHeaders
import retrofit2.Response
import java.util.regex.Pattern


fun isChunked(response: Response<*>): Boolean {
    return "chunked" == transferEncoding(response)
}

fun isSupportRange(resp: Response<*>): Boolean {
    if (!resp.isSuccessful) {
        return false
    }

    if (resp.code() == 206 || contentRange(resp).isNotEmpty() || acceptRanges(resp).isNotEmpty()) {
        return true
    }

    return false
}

fun fileName(saveName: String, url: String, response: Response<*>): String {
    if (saveName.isNotEmpty()) {
        return saveName
    }

    var fileName = contentDisposition(response)
    if (fileName.isEmpty()) {
        fileName = substringUrl(url)
    }
    return fileName
}

fun substringUrl(url: String): String {
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

    result = result.replace("/", "_", false)

    return result
}

fun contentLength(response: Response<*>): Long {
    return HttpHeaders.contentLength(response.headers())
}

private fun transferEncoding(response: Response<*>): String? {
    var header = response.headers().get("Transfer-Encoding")
    if (header == null) {
        header = ""
    }
    return header
}

fun getTotalSize(response: Response<*>): Long {
    val contentRange = contentRange(response)
    val tmp = contentRange.substringAfterLast('/')
    return tmp.toLong()
}

private fun contentRange(response: Response<*>): String {
    var header = response.headers().get("Content-Range")
    if (header == null) {
        header = ""
    }
    return header
}

private fun acceptRanges(response: Response<*>): String {
    var header = response.headers().get("Accept-Ranges")
    if (header == null) {
        header = ""
    }
    return header
}