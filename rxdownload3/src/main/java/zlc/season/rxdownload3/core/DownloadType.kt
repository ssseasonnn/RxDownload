package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import java.io.File


abstract class DownloadType(val mission: RealMission) {
    abstract fun initStatus()

    abstract fun getFile(): File?

    abstract fun download(): Flowable<out Status>

    abstract fun delete()
}