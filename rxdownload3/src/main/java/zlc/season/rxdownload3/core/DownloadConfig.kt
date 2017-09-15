package zlc.season.rxdownload3.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import zlc.season.rxdownload3.database.DbActor
import zlc.season.rxdownload3.database.EmptyDbActor
import zlc.season.rxdownload3.database.SQLiteActor

@SuppressLint("StaticFieldLeak")
object DownloadConfig {
    internal val DEBUG = true

    internal val ANY = Any()

    internal val DOWNLOADING_FILE_SUFFIX = ".download"

    internal val TMP_DIR_SUFFIX = ".TMP"
    internal val TMP_FILE_SUFFIX = ".tmp"

    internal val RANGE_DOWNLOAD_SIZE: Long = 5 * 1024 * 1024  // 5M


    internal var maxRange = 3
    internal var maxMission = 3

    internal var defaultSavePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path

    internal var enableDataBase = false
    internal var enableService = false

    internal var dbActor: DbActor = EmptyDbActor()

    internal var missionBox: MissionBox = LocalMissionBox()

    internal lateinit var context: Context


    fun init(builder: ConfigBuilder) {
        this.maxRange = builder.maxRange
        this.maxMission = builder.maxMission
        this.defaultSavePath = builder.defaultSavePath
        this.enableDataBase = builder.enableDataBase
        this.enableService = builder.enableService
        this.context = builder.context

        if (enableDataBase) {
            dbActor = SQLiteActor(context)
        }

        if (enableService) {
            missionBox = RemoteMissionBox()
        }
    }
}

class ConfigBuilder private constructor(val context: Context) {
    internal var maxRange = 3
    internal var maxMission = 3
    internal var defaultSavePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path
    internal var enableDataBase = false
    internal var enableService = false

    companion object {
        fun create(context: Context): ConfigBuilder {
            return ConfigBuilder(context.applicationContext)
        }
    }

    fun setMaxRange(max: Int): ConfigBuilder {
        this.maxRange = max
        return this
    }

    fun setMaxMission(max: Int): ConfigBuilder {
        this.maxMission = max
        return this
    }

    fun enableDataBase(enable: Boolean): ConfigBuilder {
        this.enableDataBase = enable
        return this
    }

    fun enableService(enable: Boolean): ConfigBuilder {
        this.enableService = enable
        return this
    }

    fun setDefaultPath(path: String): ConfigBuilder {
        this.defaultSavePath = path
        return this
    }
}

