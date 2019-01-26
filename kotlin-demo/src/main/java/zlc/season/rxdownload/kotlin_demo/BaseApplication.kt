package zlc.season.rxdownload.kotlin_demo

import android.app.Application
import android.os.Environment
import zlc.season.rxdownload3.core.DownloadConfig
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.extension.ApkOpenExtension


class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val builder = DownloadConfig.Builder.create(this)
                .setDebug(true)
                .enableNotification(true)
                .addExtension(ApkInstallExtension::class.java)
                .addExtension(ApkOpenExtension::class.java)
                .setDefaultPath(Environment.getExternalStorageDirectory().toString() + "/Huawei" + "/Themes")

        DownloadConfig.init(builder)
    }
}