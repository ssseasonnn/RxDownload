package zlc.season.rxdownload.kotlin_demo

import android.app.Application
import zlc.season.rxdownload3.core.DownloadConfig
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.extension.ApkOpenExtension


class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val builder = DownloadConfig.Builder.create(this)
                .setDebug(true)
                .enableDb(true)
                .setDbActor(CustomSqliteActor(this))
                .enableNotification(true)
                .addExtension(ApkInstallExtension::class.java)
                .addExtension(ApkOpenExtension::class.java)

        DownloadConfig.init(builder)
    }
}