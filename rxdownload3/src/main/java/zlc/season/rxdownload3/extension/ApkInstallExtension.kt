package zlc.season.rxdownload3.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.pm.PackageManager
import android.net.Uri.fromFile
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import android.os.Bundle
import android.support.v4.content.FileProvider.getUriForFile
import io.reactivex.Maybe
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.helper.getPackageName
import zlc.season.rxdownload3.helper.logd
import java.io.File


class ApkInstallExtension : Extension {
    lateinit var mission: RealMission
    lateinit var context: Context

    private var apkFile: File? = null
    private var apkPackageName = ""

    override fun init(mission: RealMission) {
        this.mission = mission
        if (DownloadConfig.context == null) {
            logd("No context, you should set context first")
        } else {
            this.context = DownloadConfig.context!!
        }
    }

    override fun action(): Maybe<Any> {
        return Maybe.create<Any> {
            this.apkFile = mission.getFile()
            if (apkFile == null) {
                mission.emitStatusWithNotification(Failed(Status(), ApkFileNotExistsException()))
                it.onError(ApkFileNotExistsException())
                return@create
            }

            apkPackageName = getPackageName(context, apkFile!!)

            mission.emitStatusWithNotification(Installing(mission.status))

            registerService()
            ApkInstallActivity.start(context, apkFile!!.path)

            it.onSuccess(1)
        }
    }

    private fun registerService() {
        ApkInstallService.get().subscribe {
            val packageName = it.second
            if (packageName == apkPackageName) {
                if (it.first) {
                    mission.emitStatusWithNotification(Installed(mission.status))
                } else {
                    mission.emitStatusWithNotification(Succeed(mission.status))
                }
            }
        }
    }

    object ApkInstallService {
        private val processor: FlowableProcessor<Pair<Boolean, String>> = PublishProcessor.create()

        fun dispatch(flag: Boolean, packageName: String) {
            processor.onNext(Pair(flag, packageName))
        }

        fun get(): FlowableProcessor<Pair<Boolean, String>> {
            return processor
        }
    }

    class ApkInstallActivity : Activity() {

        companion object {
            private val APK_TYPE = "application/vnd.android.package-archive"
            private val ARGS_IN_PATH = "argsInPath"
            private val RC_INSTALL_APK = 100

            fun start(context: Context, apkPath: String) {
                val intent = Intent(context, ApkInstallActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(ARGS_IN_PATH, apkPath)
                context.startActivity(intent)
            }
        }

        private lateinit var apkFile: File

        private var installTime = 0L
        private var installPackageName = ""

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val apkPath = intent.getStringExtra(ARGS_IN_PATH)
            apkFile = File(apkPath)
            installPackageName = getPackageName(this, apkFile)
            installTime = System.currentTimeMillis()

            startActivityForResult(createApkInstallIntent(), RC_INSTALL_APK)
        }

        private fun createApkInstallIntent(): Intent {
            val intent = Intent(ACTION_VIEW)
            val authority = "$packageName.rxdownload.provider"
            val uri = if (SDK_INT >= N) {
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
                check()
            }
            finish()
        }

        private fun check() {
            try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(installPackageName, 0)
                val appFile = appInfo.sourceDir
                val installedTime = File(appFile).lastModified()
                if (installedTime > installTime) {
                    ApkInstallService.dispatch(true, installPackageName)
                } else {
                    ApkInstallService.dispatch(false, installPackageName)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                ApkInstallService.dispatch(false, installPackageName)
            }
        }
    }

    class Installing(status: Status) : Status(status)

    class Installed(status: Status) : Status(status)

    class ApkFileNotExistsException : RuntimeException("Apk file not exists")
}