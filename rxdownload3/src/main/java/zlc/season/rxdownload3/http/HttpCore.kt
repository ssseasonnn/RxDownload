package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.core.DownloadConfig
import zlc.season.rxdownload3.core.RealMission
import zlc.season.rxdownload3.helper.ANY

object HttpCore {
    private const val TEST_RANGE_SUPPORT = "bytes=0-"

    private val api: RetrofitApi = RetrofitClient.get().create(RetrofitApi::class.java)

    fun checkUrl(mission: RealMission): Maybe<Any> {
        return if (DownloadConfig.useHeadMethod) {
            api.checkByHead(TEST_RANGE_SUPPORT, mission.actual.url)
        } else {
            api.checkByGet(TEST_RANGE_SUPPORT, mission.actual.url)
        }.flatMap {
            if (!it.isSuccessful) {
                throw RuntimeException(it.message())
            }
            mission.setup(it)
            return@flatMap Maybe.just(ANY)
        }
    }

    fun download(mission: RealMission, range: String = ""): Maybe<Response<ResponseBody>> {
        return api.download(range, mission.actual.url)
                .doOnSuccess {
                    if (!it.isSuccessful) {
                        throw RuntimeException(it.message())
                    }
                }
    }
}