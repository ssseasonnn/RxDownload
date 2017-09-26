package zlc.season.rxdownload3.extension

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.IntentFilter
import android.net.Uri.fromFile
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import android.os.Bundle
import android.support.v4.content.FileProvider.getUriForFile
import android.support.v4.content.LocalBroadcastManager.getInstance
import io.reactivex.Maybe
import zlc.season.rxdownload3.core.DownloadConfig
import zlc.season.rxdownload3.core.RealMission
import zlc.season.rxdownload3.core.Status
import zlc.season.rxdownload3.core.Succeed
import zlc.season.rxdownload3.extension.ApkInstallExtension.ApkInstallActivity.Companion.ACTION_APK_INSTALL_CANCEL
import zlc.season.rxdownload3.helper.getPackageName
import java.io.File


class ApkInstallExtension : Extension {
    private val SCHEME = "package"

    lateinit var mission: RealMission
    lateinit var context: Context

    private var apkFile: File? = null
    private var installApkPackageName = ""

    private var successReceiver = ApkInstallSuccessReceiver()
    private var cancelReceiver = ApkInstallCancelReceiver()

    override fun init(mission: RealMission) {
        this.mission = mission
        this.context = DownloadConfig.context
    }

    override fun action(): Maybe<Any> {
        return Maybe.create<Any> {
            this.apkFile = mission.getFile()
            if (apkFile == null) {
                it.onError(RuntimeException("Apk file is null"))
                return@create
            }

            installApkPackageName = getPackageName(context, apkFile!!)

            mission.emitStatusWithNotification(Installing(mission.getStatus()))

            registerReceiver()
            ApkInstallActivity.start(context, apkFile!!.path)

            it.onSuccess(1)
        }
    }

    private fun registerReceiver() {
        val success = IntentFilter()
        success.addAction(ACTION_PACKAGE_ADDED)
        success.addDataScheme(SCHEME)
        getInstance(context).registerReceiver(successReceiver, success)

        val cancel = IntentFilter(ACTION_APK_INSTALL_CANCEL)
        getInstance(context).registerReceiver(cancelReceiver, cancel)
    }

    class ApkInstallActivity : Activity() {

        companion object {
            private val APK_TYPE = "application/vnd.android.package-archive"
            private val ARGS_IN_PATH = "argsInPath"
            private val RC_INSTALL_APK = 100

            val ARGS_OUT_PACKAGE_NAME = "argsOutPackageName"
            val ACTION_APK_INSTALL_CANCEL = "actionApkInstallCancel"

            fun start(context: Context, apkPath: String) {
                val intent = Intent(context, ApkInstallActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(ARGS_IN_PATH, apkPath)
                context.startActivity(intent)
            }
        }

        lateinit var apkFile: File

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val apkPath = intent.getStringExtra(ARGS_IN_PATH)
            apkFile = File(apkPath)

            startActivityForResult(createApkInstallIntent(), RC_INSTALL_APK)
        }

        private fun createApkInstallIntent(): Intent {
            val intent = Intent(ACTION_VIEW)
            val authority = "$packageName.rxdownload.provider"
            val uri = if (SDK_INT > N) {
                getUriForFile(this, authority, apkFile)
            } else {
                fromFile(apkFile)
            }
            intent.setDataAndType(uri, APK_TYPE)
            intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
            return intent
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == RC_INSTALL_APK) {
                if (resultCode == RESULT_CANCELED) {
                    val intent = Intent(ACTION_APK_INSTALL_CANCEL)
                    intent.putExtra(ARGS_OUT_PACKAGE_NAME, getPackageName(this, apkFile))
                    getInstance(this).sendBroadcast(intent)
                }
            }
            finish()
        }
    }

    inner class ApkInstallSuccessReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            val action = intent.action
            val data = intent.data
            if (action == null || data == null) return

            val receivePackageName = data.encodedSchemeSpecificPart

            if (installApkPackageName == receivePackageName) {
                if (action == ACTION_PACKAGE_ADDED) {
                    mission.emitStatusWithNotification(Installed(mission.getStatus()))
                    getInstance(context).unregisterReceiver(this)
                }
            }
        }
    }

    inner class ApkInstallCancelReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return

            val action = intent.action ?: return
            val receivePackageName = intent.getStringExtra(ApkInstallActivity.ARGS_OUT_PACKAGE_NAME)

            if (installApkPackageName == receivePackageName) {
                if (action == ApkInstallActivity.ACTION_APK_INSTALL_CANCEL) {
                    mission.emitStatusWithNotification(Succeed(mission.getStatus()))
                    getInstance(context).unregisterReceiver(this)
                }
            }
        }
    }

    class Installing(status: Status) : Status(status)

    class Installed(status: Status) : Status(status)
}