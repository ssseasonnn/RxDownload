package zlc.season.rxdownload4.downloader

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.Status
import zlc.season.rxdownload4.task.Task

interface Downloader {
    fun download(task: Task, response: Response<ResponseBody>): Flowable<Status>
}