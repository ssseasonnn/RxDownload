package zlc.season.rxdownload.kotlin_demo

import android.app.Application
import zlc.season.rxdownload3.core.DownloadConfig
import zlc.season.rxdownload3.extension.ApkInstallExtension


class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val builder = DownloadConfig.Builder.create(this)
                .enableService(true)
                .enableNotification(true)
                .addExtension(ApkInstallExtension::class.java)

        DownloadConfig.init(builder)
    }
}