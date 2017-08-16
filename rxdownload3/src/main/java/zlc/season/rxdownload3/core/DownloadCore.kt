package zlc.season.rxdownload3.core

import android.os.Environment.*
import io.reactivex.Observable
import io.reactivex.plugins.RxJavaPlugins
import zlc.season.rxdownload3.helper.Logger
import zlc.season.rxdownload3.http.RetrofitApi
import zlc.season.rxdownload3.http.RetrofitClient
import java.io.InterruptedIOException
import java.net.SocketException


class DownloadCore {
    var api = RetrofitClient.get().create(RetrofitApi::class.java)
    var defaultSavePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path
    var retryCount = 3
    var threadCount = 3


    init {
        initRxJavaPlugin()
    }

    private fun initRxJavaPlugin() {
        RxJavaPlugins.setErrorHandler {
            t: Throwable ->
            if (t is InterruptedException) {
                Logger.loge("InterruptedException", t)
            } else if (t is InterruptedIOException) {
                Logger.loge("InterruptedIOException", t)
            } else if (t is SocketException) {
                Logger.loge("SocketException", t)
            }
        }
    }


    fun processMission(mission: Mission): Observable<DownloadStatus> {

    }


}