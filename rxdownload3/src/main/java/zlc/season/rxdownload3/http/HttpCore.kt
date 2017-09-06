package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.core.IllegalUrlException
import zlc.season.rxdownload3.core.RealMission


object HttpCore {
    private val TEST_RANGE_SUPPORT = "bytes=0-"
    private val api: RetrofitApi = RetrofitClient.get().create(RetrofitApi::class.java)

    fun checkUrl(mission: RealMission): Maybe<Any> {
        return api.check(TEST_RANGE_SUPPORT, mission.actual.url)
                .flatMap {
                    if (!it.isSuccessful) {
                        throw IllegalUrlException("Url is illegal, please make sure your url correct")
                    }
                    mission.setup(it)
                    Maybe.just(1)
                }
    }

    fun download(realMission: RealMission, range: String = ""): Maybe<Response<ResponseBody>> {
        return api.download(range, realMission.actual.url)
    }


}