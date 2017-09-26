package zlc.season.rxdownload3.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import zlc.season.rxdownload3.database.DbActor
import zlc.season.rxdownload3.database.SQLiteActor
import zlc.season.rxdownload3.extension.Extension
import zlc.season.rxdownload3.http.OkHttpClientFactory
import zlc.season.rxdownload3.http.OkHttpClientFactoryImpl
import zlc.season.rxdownload3.notification.NotificationFactory
import zlc.season.rxdownload3.notification.NotificationFactoryImpl

@SuppressLint("StaticFieldLeak")
object DownloadConfig {
    internal val DEBUG = true

    internal val ANY = Any()

    internal val DOWNLOADING_FILE_SUFFIX = ".download"

    internal val TMP_DIR_SUFFIX = ".TMP"
    internal val TMP_FILE_SUFFIX = ".tmp"

    internal val RANGE_DOWNLOAD_SIZE: Long = 5 * 1024 * 1024  // 5M

    internal var maxRange = 3

    internal var defaultSavePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path

    internal lateinit var context: Context

    internal var enableDb = false
    internal lateinit var dbActor: DbActor

    internal var missionBox: MissionBox = LocalMissionBox()

    internal var enableNotification = false

    internal var notificationFactory: NotificationFactory = NotificationFactoryImpl()

    internal var okHttpClientFactory: OkHttpClientFactory = OkHttpClientFactoryImpl()

    internal var extensions = mutableListOf<Class<out Extension>>()

    fun init(builder: Builder) {
        this.context = builder.context
        this.maxRange = builder.maxRange
        this.defaultSavePath = builder.defaultSavePath

        this.enableDb = builder.enableDb
        this.dbActor = builder.dbActor

        this.enableNotification = builder.enableNotification
        this.notificationFactory = builder.notificationFactory

        this.okHttpClientFactory = builder.okHttpClientFactory

        this.extensions = builder.extensions

        val enableService = builder.enableService
        if (enableService) {
            missionBox = RemoteMissionBox()
        }
    }

    class Builder private constructor(val context: Context) {
        internal var maxRange = 3
        internal var defaultSavePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path

        internal var enableDb = false
        internal var dbActor: DbActor = SQLiteActor(context)

        internal var enableService = false

        internal var enableNotification = false
        internal var notificationFactory: NotificationFactory = NotificationFactoryImpl()

        internal var okHttpClientFactory: OkHttpClientFactory = OkHttpClientFactoryImpl()

        internal var extensions = mutableListOf<Class<out Extension>>()

        companion object {
            fun create(context: Context): Builder {
                return Builder(context.applicationContext)
            }
        }

        fun setMaxRange(max: Int): Builder {
            this.maxRange = max
            return this
        }

        fun enableService(enable: Boolean): Builder {
            this.enableService = enable
            return this
        }

        fun enableNotification(enable: Boolean): Builder {
            this.enableNotification = enable
            return this
        }

        fun setNotificationFactory(notificationFactory: NotificationFactory): Builder {
            this.notificationFactory = notificationFactory
            return this
        }

        fun setDefaultPath(path: String): Builder {
            this.defaultSavePath = path
            return this
        }

        fun enableDb(enable: Boolean): Builder {
            this.enableDb = enableDb
            return this
        }

        fun setDbActor(dbActor: DbActor): Builder {
            this.dbActor = dbActor
            return this
        }

        fun setOkHttpClientFacotry(okHttpClientFactory: OkHttpClientFactory): Builder {
            this.okHttpClientFactory = okHttpClientFactory
            return this
        }

        fun addExtension(extension: Class<out Extension>): Builder {
            this.extensions.add(extension)
            return this
        }
    }
}



