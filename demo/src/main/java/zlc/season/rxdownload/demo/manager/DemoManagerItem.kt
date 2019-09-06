package zlc.season.rxdownload.demo.manager

import android.content.Context
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import zlc.season.rxdownload.demo.R
import zlc.season.rxdownload.demo.utils.createTaskManager
import zlc.season.rxdownload.demo.utils.gone
import zlc.season.rxdownload.demo.utils.visible
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.task.Task
import zlc.season.yasha.YashaItem

class DemoManagerItem(
        val task: Task,
        val status: Status
) : YashaItem {

    private var tag: Any? = null

    fun subscribe(
            progressBar: ProgressBar,
            statusTv: TextView,
            percentTv: TextView,
            startIv: ImageView,
            pauseIv: ImageView,
            cancelIv: ImageView,
            moreIv: ImageView,
            context: Context
    ) {
        val taskManager = task.createTaskManager()

        tag = taskManager.subscribe {
            renderStatus(it, progressBar, statusTv, percentTv, startIv, pauseIv, cancelIv, moreIv, context)
        }
    }

    fun renderStatus(
            status: Status,
            progressBar: ProgressBar,
            statusTv: TextView,
            percentTv: TextView,
            startIv: ImageView,
            pauseIv: ImageView,
            cancelIv: ImageView,
            moreIv: ImageView,
            context: Context
    ) {
        progressBar.progress = status.progress.percent().toInt()
        progressBar.max = 100
        progressBar.isIndeterminate = status.progress.isChunked

        statusTv.text = stateStr(context, status)
        percentTv.text = status.progress.percentStr()

        when (status) {
            is Started,
            is Downloading -> {
                startIv.gone()
                pauseIv.visible()
                cancelIv.visible()
                moreIv.gone()
                progressBar.visible()
            }
            is Normal,
            is Paused,
            is Failed -> {
                startIv.visible()
                pauseIv.gone()
                cancelIv.visible()
                moreIv.gone()
                progressBar.visible()
            }
            is Completed -> {
                percentTv.gone()
                startIv.gone()
                pauseIv.gone()
                cancelIv.gone()
                moreIv.visible()
                progressBar.gone()
            }
        }
    }

    fun dispose() {
        tag?.let {
            task.createTaskManager().dispose(it)
        }
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

    fun delete() {
        task.createTaskManager().delete()
    }

    override fun cleanUp() {
        dispose()
    }

    private fun stateStr(context: Context, status: Status): String {
        return when (status) {
            is Normal -> ""
            is Started -> context.getString(R.string.started_text)
            is Downloading -> context.getString(R.string.started_text)
            is Paused -> context.getString(R.string.paused_text)
            is Completed -> context.getString(R.string.completed_text)
            is Failed -> context.getString(R.string.failed_text)
            is Deleted -> ""
            else -> ""
        }
    }
}