package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import zlc.season.rxdownload3.core.IllegalUrlException
import zlc.season.rxdownload3.core.MissionWrapper


class HttpProcessor {
    val api: RetrofitApi = RetrofitClient.get().create(RetrofitApi::class.java)

    fun checkUrlWithHEAD(missionWrapper: MissionWrapper): Maybe<Any> {
        return api.checkUrlWithHEAD(missionWrapper.mission.provideUrl())
                .flatMap { resp ->
                    if (resp.isSuccessful) {
                        Maybe.just(1)
                    } else {
                        checkUrlWithGET(missionWrapper)
                    }
                }
    }

    fun checkUrlWithGET(missionWrapper: MissionWrapper): Maybe<Any> {
        return api.checkUrlWithGET(missionWrapper.mission.provideUrl())
                .flatMap { resp ->
                    if (resp.isSuccessful) {
                        Maybe.just(1)
                    } else {
                        throw IllegalUrlException("Url is illegal, please check your url correct")
                    }
                }
    }
}