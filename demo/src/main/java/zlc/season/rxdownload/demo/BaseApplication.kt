package zlc.season.rxdownload.demo

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import zlc.season.rxdownload4.utils.log


class BaseApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        RxJavaPlugins.setErrorHandler {
            if (it is UndeliverableException) {
                //do nothing
            } else {
                it.log()
            }
        }
    }
}