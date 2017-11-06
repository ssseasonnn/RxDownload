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
    internal var DEBUG = false

    internal val DOWNLOADING_FILE_SUFFIX = ".download"
    internal val TMP_DIR_SUFFIX = ".TMP"
    internal val TMP_FILE_SUFFIX = ".tmp"

    internal val RANGE_DOWNLOAD_SIZE: Long = 4 * 1024 * 1024  //4M

    internal var maxMission = 3
    internal var maxRange = Runtime.getRuntime().availableProcessors() + 1

    internal var defaultSavePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path

    var context: Context? = null

    internal var fps = 30

    internal var enableDb = false
    internal lateinit var dbActor: DbActor

    internal var missionBox: MissionBox = LocalMissionBox()

    internal var enableNotification = false

    internal lateinit var notificationFactory: NotificationFactory

    internal var okHttpClientFactory: OkHttpClientFactory = OkHttpClientFactoryImpl()

    internal var extensions = mutableListOf<Class<out Extension>>()

    fun init(builder: Builder) {
        this.context = builder.context

        this.DEBUG = builder.debug

        this.fps = builder.fps
        this.maxMission = builder.maxMission
        this.maxRange = builder.maxRange
        this.defaultSavePath = builder.defaultSavePath

        this.enableDb = builder.enableDb
        this.dbActor = builder.dbActor

        if (enableDb) {
            dbActor.init()
        }

        this.enableNotification = builder.enableNotification
        this.notificationFactory = builder.notificationFactory

        this.okHttpClientFactory = builder.okHttpClientFactory

        this.extensions = builder.extensions

        val enableService = builder.enableService
        this.missionBox = if (enableService) {
            RemoteMissionBox()
        } else {
            LocalMissionBox()
        }
    }

    class Builder private constructor(val context: Context) {
        internal var maxMission = 3
        internal var maxRange = Runtime.getRuntime().availableProcessors() + 1

        internal var debug = true

        internal var fps = 30
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

        fun setDebug(debug: Boolean): Builder {
            this.debug = debug
            return this
        }

        fun setMaxMission(max: Int): Builder {
            this.maxMission = max
            return this
        }

        fun setMaxRange(max: Int): Builder {
            this.maxRange = max
            return this
        }

        /**
         * Set fps. Default is 30.
         *
         * Note that this value is too large will cause the interface to stuck
         */
        fun setFps(fps: Int): Builder {
            this.fps = fps
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
            this.enableDb = enable
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



