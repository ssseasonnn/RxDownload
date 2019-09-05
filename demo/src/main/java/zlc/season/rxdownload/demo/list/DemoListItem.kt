package zlc.season.rxdownload.demo.list

import android.content.Context
import zlc.season.rxdownload.demo.R
import zlc.season.rxdownload.demo.utils.ProgressButton
import zlc.season.rxdownload.demo.utils.installApk
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.notification.SimpleNotificationCreator
import zlc.season.rxdownload4.recorder.RoomRecorder
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
            is Deleted -> taskManager.start()
        }
    }

    fun subscribe(btn_action: ProgressButton, context: Context) {
        val taskManager = url.manager(notificationCreator = SimpleNotificationCreator(),
                recorder = RoomRecorder())

        btn_action.text = stateStr(context)
        btn_action.setStatus(taskManager.currentStatus())

        taskManager.subscribe {
            btn_action.setStatus(it)
            btn_action.text = stateStr(context)
        }
    }

    private fun stateStr(context: Context): String {
        return when (url.manager().currentStatus()) {
            is Normal -> context.getString(R.string.start_text)
            is Started -> context.getString(R.string.pause_text)
            is Downloading -> context.getString(R.string.pause_text)
            is Paused -> context.getString(R.string.continue_text)
            is Completed -> context.getString(R.string.install_text)
            is Failed -> context.getString(R.string.retry_text)
            is Deleted -> context.getString(R.string.start_text)
        }
    }

    fun dispose() {
        url.manager().dispose()
    }

    override fun cleanUp() {
        dispose()
    }
}