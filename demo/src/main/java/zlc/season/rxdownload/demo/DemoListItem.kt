package zlc.season.rxdownload.demo

import android.content.Context
import zlc.season.rxdownload.demo.utils.ProgressButton
import zlc.season.rxdownload.demo.utils.installApk
import zlc.season.rxdownload4.manager.*
import zlc.season.yasha.YashaItem

class DemoListItem(
        val name: String,
        val icon: String,
        val url: String,
        val size: String
) : YashaItem {

    fun action(context: Context) {
        val taskManager = url.manager()
        when (taskManager.currentStatus()) {
            is Normal -> taskManager.start()
            is Started -> taskManager.stop()
            is Downloading -> taskManager.stop()
            is Failed -> taskManager.start()
            is Paused -> taskManager.start()
            is Completed -> context.installApk(taskManager.file())
        }
    }

    fun subscribe(btn_action: ProgressButton, context: Context) {
        val taskManager = url.manager()

        btn_action.text = stateStr(context)
        btn_action.setProgress(taskManager.currentProgress().downloadSize,
                taskManager.currentProgress().totalSize)

        taskManager.subscribe {
            btn_action.setProgress(
                    it.progress.downloadSize,
                    it.progress.totalSize
            )

            btn_action.text = stateStr(context)
        }
    }

    private fun stateStr(context: Context): String {
        return when (url.manager().currentStatus()) {
            is Normal -> context.getString(R.string.download)
            is Started -> context.getString(R.string.pause)
            is Downloading -> context.getString(R.string.pause)
            is Paused -> context.getString(R.string.continue_text)
            is Completed -> context.getString(R.string.install)
            is Failed -> context.getString(R.string.retry)
        }
    }

    fun dispose() {
        url.manager().dispose()
    }

    override fun cleanUp() {
        dispose()
    }
}