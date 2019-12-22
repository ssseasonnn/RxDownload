package zlc.season.rxdownload.demo.manager

import android.content.Context
import kotlinx.android.synthetic.main.demo_download_list_item.*
import zlc.season.rxdownload.demo.R
import zlc.season.rxdownload.demo.utils.createTaskManager
import zlc.season.rxdownload.demo.utils.gone
import zlc.season.rxdownload.demo.utils.visible
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.task.Task
import zlc.season.yasha.YashaItem
import zlc.season.yasha.YashaScope

class DemoManagerItem(
        val task: Task,
        val lastStatus: Status
) : YashaItem {

    private var tag: Any? = null

    fun subscribe(
            scope: YashaScope<DemoManagerItem>,
            context: Context
    ) {
        val taskManager = task.createTaskManager()

        renderStatus(scope, lastStatus, context)

        tag = taskManager.subscribe {
            renderStatus(scope, it, context)
        }
    }

    private fun renderStatus(scope: YashaScope<DemoManagerItem>, it: Status, context: Context) {
        scope.apply {
            progress_bar.progress = it.progress.percent().toInt()
            progress_bar.max = 100
            progress_bar.isIndeterminate = it.progress.isChunked

            tv_status.text = stateStr(context, it)
            tv_percent.text = it.progress.percentStr()

            when (it) {
                is Normal -> {
                    // do nothing
                }
                is Pending,
                is Started,
                is Downloading -> {
                    btn_start.gone()
                    btn_pause.visible()
                    btn_cancel.visible()
                    btn_more.gone()

                    progress_bar.visible()
                    tv_percent.visible()
                }
                is Paused,
                is Failed -> {
                    btn_start.visible()
                    btn_pause.gone()
                    btn_cancel.visible()
                    btn_more.gone()

                    progress_bar.visible()
                    tv_percent.visible()
                }
                is Completed -> {
                    btn_start.gone()
                    btn_pause.gone()
                    btn_cancel.gone()
                    btn_more.visible()

                    progress_bar.gone()
                    tv_percent.gone()
                }
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
            is Pending -> context.getString(R.string.pending_text)
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