package zlc.season.rxdownload4.downloader

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.task.TaskInfo

interface Downloader {
    fun download(taskInfo: TaskInfo, response: Response<ResponseBody>): Flowable<Progress>
}