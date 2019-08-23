package zlc.season.rxdownload.demo

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.rxdownload.demo.utils.ProgressButton
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.safeDispose
import zlc.season.yasha.YashaItem

class DemoListItem(
        val name: String,
        val icon: String,
        val url: String,
        val size: String
) : YashaItem {
    var disposable: Disposable? = null

    fun stateStr(): String {
        return when (Task(url).manager().currentStatus()) {
            is Normal -> "下载"
            is Started -> "暂停"
            is Paused -> "继续"
            is Completed -> "安装"
            is Failed -> "重试"
            is Downloading -> "暂停"
        }
    }

    fun action() {
        val taskManager = Task(url).manager()
        when (taskManager.currentStatus()) {
            is Normal -> taskManager.start()
            is Started -> taskManager.stop()
            is Downloading -> taskManager.stop()
            is Completed -> {
            }
            is Failed -> taskManager.start()
            is Paused -> taskManager.start()
        }
    }

    fun subscribe(btn_action: ProgressButton) {
        val taskManager = Task(url).manager()

        btn_action.setProgress(taskManager.currentProgress().downloadSize,
                taskManager.currentProgress().totalSize)

        disposable = taskManager.status()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { status ->
                    if (status is Downloading) {
                        btn_action.setProgress(
                                status.progress.downloadSize,
                                status.progress.totalSize
                        )
                    }

                    btn_action.text = stateStr()
                }
    }

    fun dispose() {
        disposable.safeDispose()
    }

    override fun cleanUp() {
        disposable.safeDispose()
    }
}