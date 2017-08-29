package zlc.season.rxdownload3.http

import io.reactivex.Maybe
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload3.core.IllegalUrlException
import zlc.season.rxdownload3.core.MissionWrapper
import zlc.season.rxdownload3.helper.ResponseUtil


object HttpProcessor {
    val TEST_RANGE_SUPPORT = "bytes=0-"

    val api: RetrofitApi = RetrofitClient.get().create(RetrofitApi::class.java)

    fun checkUrl(missionWrapper: MissionWrapper): Maybe<Any> {
        return api.check(TEST_RANGE_SUPPORT, "", missionWrapper.mission.url())
                .flatMap { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalUrlException("Url is illegal, please make sure your url legal")
                    }

                    missionWrapper.contentLength = ResponseUtil.contentLength(resp)

                    if (ResponseUtil.isSupportRange(resp)) {
                        missionWrapper.isSupportRange = false
                    }

                    if (resp.code() == 206) {
                        missionWrapper.isFileChange = true
                    }

                    if (resp.code() == 304) {
                        missionWrapper.isFileChange = false
                    }

                    if (missionWrapper.realFileName.isEmpty()) {
                        var fileName = ResponseUtil.contentDisposition(resp)
                        if (fileName.isEmpty()) {
                            fileName = ResponseUtil.fileName(missionWrapper.mission.url())
                        }
                        missionWrapper.realFileName = fileName
                    }

                    Maybe.just(1)
                }
    }

    fun download(missionWrapper: MissionWrapper, range: String = ""): Maybe<Response<ResponseBody>> {
        return api.download(range, missionWrapper.mission.url())
    }


}