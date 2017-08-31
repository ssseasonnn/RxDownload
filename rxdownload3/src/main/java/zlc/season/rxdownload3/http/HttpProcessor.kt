package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.core.IllegalUrlException
import zlc.season.rxdownload3.core.RealMission
import zlc.season.rxdownload3.helper.ResponseUtil


object HttpProcessor {
    val TEST_RANGE_SUPPORT = "bytes=0-"

    val api: RetrofitApi = RetrofitClient.get().create(RetrofitApi::class.java)

    fun checkUrl(mission: RealMission): Maybe<Any> {
        return api.check(TEST_RANGE_SUPPORT, "", mission.mission.url())
                .flatMap { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalUrlException("Url is illegal, please make sure your url legal")
                    }

                    mission.contentLength = ResponseUtil.contentLength(resp)

                    println(ResponseUtil.isSupportRange(resp))

                    if (ResponseUtil.isSupportRange(resp)) {
                        mission.isSupportRange = true
                    }

                    if (resp.code() == 206) {
                        mission.isFileChange = true
                    }

                    if (resp.code() == 304) {
                        mission.isFileChange = false
                    }

                    if (mission.realFileName.isEmpty()) {
                        var fileName = ResponseUtil.contentDisposition(resp)
                        if (fileName.isEmpty()) {
                            fileName = ResponseUtil.fileName(mission.mission.url())
                        }
                        mission.realFileName = fileName
                    }

                    Maybe.just(1)
                }
    }

    fun download(realMission: RealMission, range: String = ""): Maybe<Response<ResponseBody>> {
        return api.download(range, realMission.mission.url())
    }


}