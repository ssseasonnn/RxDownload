package zlc.season.rxdownload.demo.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.graphics.drawable.Drawable
import android.net.Uri.fromFile
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import android.support.v4.content.FileProvider.getUriForFile
import android.support.v4.view.ViewCompat
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import zlc.season.rxdownload4.manager.TaskManager
import zlc.season.rxdownload4.manager.manager
import zlc.season.rxdownload4.notification.SimpleNotificationCreator
import zlc.season.rxdownload4.recorder.RoomRecorder
import zlc.season.rxdownload4.task.Task
import java.io.File

fun Context.installApk(file: File) {
    val intent = Intent(ACTION_VIEW)
    val authority = "$packageName.rxdownload.demo.provider"
    val uri = if (SDK_INT >= N) {
        getUriForFile(this, authority, file)
    } else {
        fromFile(file)
    }
    intent.setDataAndType(uri, "application/vnd.android.package-archive")
    intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(intent)
}

fun View.click(block: () -> Unit) {
    setOnClickListener {
        block()
    }
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun ImageView.load(url: String) {
    Glide.with(this).load(url).into(this)
}

fun View.background(drawable: Drawable) {
    ViewCompat.setBackground(this, drawable)
}

fun Activity.start(clazz: Class<*>) {
    startActivity(Intent(this, clazz))
}


fun Task.createTaskManager(): TaskManager {
    return manager(
            notificationCreator = SimpleNotificationCreator(),
            recorder = RoomRecorder()
    )
}