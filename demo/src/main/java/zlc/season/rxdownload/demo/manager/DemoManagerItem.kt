package zlc.season.rxdownload.demo.manager

import android.content.Context
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import zlc.season.rxdownload.demo.R
import zlc.season.rxdownload.demo.utils.createTaskManager
import zlc.season.rxdownload.demo.utils.gone
import zlc.season.rxdownload.demo.utils.installApk
import zlc.season.rxdownload.demo.utils.visible
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.task.Task
import zlc.season.yasha.YashaItem

class DemoManagerItem(
        val task: Task
) : YashaItem {
    fun action(context: Context) {
        val taskManager = task.createTaskManager()
        when (taskManager.currentStatus()) {
            is Normal -> taskManager.start()
            is Started -> taskManager.stop()
            is Downloading -> taskManager.stop()
            is Failed -> taskManager.start()
            is Paused -> taskManager.start()
            is Completed -> context.installApk(taskManager.file())
            is Deleted -> taskManager.start()
        }
    }

    fun subscribe(
            progressBar: ProgressBar,
            statusTv: TextView,
            percentTv: TextView,
            startIv: ImageView,
            pauseIv: ImageView,
            context: Context
    ) {
        val taskManager = task.createTaskManager()

        taskManager.subscribe {
            progressBar.progress = it.progress.downloadSize.toInt()
            progressBar.max = it.progress.totalSize.toInt()
            progressBar.isIndeterminate = it.progress.isChunked

            statusTv.text = stateStr(context, it)
            percentTv.text = it.progress.percentStr()

            when (it) {
                is Started,
                is Downloading -> {
                    startIv.gone()
                    pauseIv.visible()
                }
                is Normal,
                is Paused,
                is Failed -> {
                    startIv.visible()
                    pauseIv.gone()
                }
                is Completed -> {
                    startIv.gone()
                    pauseIv.gone()
                }
            }
        }
    }

    fun dispose() {
        task.createTaskManager().dispose()
    }

    fun cancel() {
        task.createTaskManager().delete()
    }

    fun start() {
        task.createTaskManager().start()
    }

    fun stop() {
        task.createTaskManager().stop()
    }

    override fun cleanUp() {
        dispose()
    }

    private fun stateStr(context: Context, status: Status): String {
        return when (status) {
            is Normal -> context.getString(R.string.start_text)
            is Started -> context.getString(R.string.pause_text)
            is Downloading -> context.getString(R.string.pause_text)
            is Paused -> context.getString(R.string.continue_text)
            is Completed -> context.getString(R.string.install_text)
            is Failed -> context.getString(R.string.retry_text)
            is Deleted -> context.getString(R.string.start_text)
        }
    }
}