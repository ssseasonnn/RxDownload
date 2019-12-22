package zlc.season.rxdownload.demo.list

import android.content.Context
import zlc.season.rxdownload.demo.R
import zlc.season.rxdownload.demo.utils.ProgressButton
import zlc.season.rxdownload.demo.utils.createTaskManager
import zlc.season.rxdownload.demo.utils.installApk
import zlc.season.rxdownload4.manager.*
import zlc.season.yasha.YashaItem

class DemoListItem(
        val name: String,
        val icon: String,
        val url: String,
        val size: String
) : YashaItem {

    private var tag: Any? = null

    fun action(context: Context) {
        val taskManager = url.createTaskManager()
        when (taskManager.currentStatus()) {
            is Normal -> taskManager.start()
            is Pending -> taskManager.stop()
            is Started -> taskManager.stop()
            is Downloading -> taskManager.stop()
            is Failed -> taskManager.start()
            is Paused -> taskManager.start()
            is Completed -> context.installApk(taskManager.file())
            is Deleted -> taskManager.start()
        }
    }

    fun subscribe(btn_action: ProgressButton, context: Context) {
        val taskManager = url.createTaskManager()

        val currentStatus = taskManager.currentStatus()
        btn_action.setStatus(currentStatus)
        btn_action.text = stateStr(context, currentStatus)

        tag = taskManager.subscribe {
            btn_action.setStatus(it)
            btn_action.text = stateStr(context, it)
        }
    }

    private fun stateStr(context: Context, status: Status): String {
        return when (status) {
            is Normal -> context.getString(R.string.start_text)
            is Pending -> context.getString(R.string.pending_text)
            is Started -> context.getString(R.string.pause_text)
            is Downloading -> context.getString(R.string.pause_text)
            is Paused -> context.getString(R.string.continue_text)
            is Completed -> context.getString(R.string.install_text)
            is Failed -> context.getString(R.string.retry_text)
            is Deleted -> context.getString(R.string.start_text)
            else -> ""
        }
    }

    fun dispose() {
        tag?.let {
            url.createTaskManager().dispose(it)
        }
    }

    override fun cleanUp() {
        dispose()
    }
}