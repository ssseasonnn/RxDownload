package zlc.season.rxdownload3.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri.fromFile
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import android.support.v4.content.FileProvider.getUriForFile
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.DownloadConfig
import zlc.season.rxdownload3.core.RealMission
import zlc.season.rxdownload3.core.Status
import java.io.File


class ApkInstallExtension : BroadcastReceiver(), Extension {
    private val SCHEME = "package"
    private val APK_TYPE = "application/vnd.android.package-archive"

    lateinit var mission: RealMission
    lateinit var context: Context
    private var apkFile: File? = null

    override fun init(mission: RealMission) {
        this.mission = mission
        this.context = DownloadConfig.context
    }

    override fun action(): Maybe<Any> {
        return Maybe.create {
            this.apkFile = mission.getFile()
            if (apkFile == null) {
                it.onError(RuntimeException("Apk file is null"))
                return@create
            }

            mission.emitStatusWithNotification(Installing(mission.getStatus()))

            registerReceiver()
            installApk()

            it.onSuccess(1)
        }
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_PACKAGE_ADDED)
        intentFilter.addDataScheme(SCHEME)
        context.registerReceiver(this, intentFilter)
    }

    private fun installApk() {
        val authority = "${context.packageName}.rxdownload.provider"
        val intent = Intent(ACTION_VIEW)
        val uri = if (SDK_INT > N) {
            getUriForFile(context, authority, apkFile)
        } else {
            fromFile(apkFile)
        }
        intent.setDataAndType(uri, APK_TYPE)
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(FLAG_GRANT_WRITE_URI_PERMISSION)
        context.startActivity(intent)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context as Context
        intent as Intent

        val action = intent.action
        val uri = intent.data

        if (action == null || uri == null) {
            return
        }

        if (apkFile == null) {
            return
        }

        val pm = context.packageManager
        val apkInfo = pm.getPackageArchiveInfo(apkFile!!.path, PackageManager.GET_ACTIVITIES)

        val installPackageName = apkInfo.packageName
        val receivePackageName = uri.encodedSchemeSpecificPart

        if (installPackageName == receivePackageName) {
            if (action == ACTION_PACKAGE_ADDED) {
                mission.emitStatusWithNotification(Installed(mission.getStatus()))
                context.unregisterReceiver(this)
            }
        }
    }

    class Installing(status: Status) : Status(status)

    class Installed(status: Status) : Status(status)
}