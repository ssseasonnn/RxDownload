package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import retrofit2.Response
import zlc.season.rxdownload3.core.IllegalUrlException
import zlc.season.rxdownload3.core.MissionWrapper


object HttpProcessor {
    val TEST_RANGE_SUPPORT = "bytes=0-1"

    val api: RetrofitApi = RetrofitClient.get().create(RetrofitApi::class.java)

    fun checkUrl(missionWrapper: MissionWrapper): Maybe<Any> {
        return api.check(TEST_RANGE_SUPPORT, "", missionWrapper.mission.provideUrl())
                .flatMap { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalUrlException("Url is illegal, please make sure your url legal")
                    }

                    if (resp.code() == 206) {
                        TODO("file changed")
                    }

                    if (resp.code() == 304) {
                        TODO("file not change")
                    }

                    Maybe.just(1)
                }
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