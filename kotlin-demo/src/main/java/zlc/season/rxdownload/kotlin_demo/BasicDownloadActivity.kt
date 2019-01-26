package zlc.season.rxdownload.kotlin_demo

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_basic_download.*
import kotlinx.android.synthetic.main.content_basic_download.*
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.helper.dispose

class BasicDownloadActivity : AppCompatActivity() {

    val TAG = "BasicDownloadActivity"

    private var disposable: Disposable? = null
    private var currentStatus = Status()

    val mission = Mission(url, "PANO.JPG", Environment.getExternalStorageDirectory().toString() + "/Huawei" + "/Themes", true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_download)
        setSupportActionBar(toolbar)

        loadImg()
        setAction()
        create()
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose(disposable)
    }

    private fun loadImg() {
        Picasso.with(this).load(iconUrl).into(img)
    }

    private fun setAction() {
        action.setOnClickListener {
            when (currentStatus) {
                is Normal -> start()
                is Suspend -> start()
                is Failed -> start()
                is Downloading -> stop()
                is Succeed -> start()
                is ApkInstallExtension.Installed -> open()
            }
        }

        finish.setOnClickListener { finish() }
    }

    private fun create() {
        disposable = RxDownload.create(mission, autoStart = false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    currentStatus = status
                    setProgress(status)
                    setActionText(status)
                    Log.i(TAG, "status $status")

                }
    }

    private fun setProgress(status: Status) {
        progress.max = status.totalSize.toInt()
        progress.progress = status.downloadSize.toInt()

        percent.text = status.percent()
        size.text = status.formatString()
    }

    private fun setActionText(status: Status) {
        val text = when (status) {
            is Normal -> "Normal"
            is Suspend -> "Suspend"
            is Waiting -> "Waiting"
            is Downloading -> "Downloading"
            is Failed -> "Failed ${status.throwable}"
            is Deleted -> "Deleted"
            is Succeed -> "Succeed"
            is ApkInstallExtension.Installing -> "Installing"
            is ApkInstallExtension.Installed -> "Installed"
            else -> ""
        }
        action.text = text
    }

    private fun start() {
        RxDownload.start(mission).subscribe()
    }

    private fun stop() {
        RxDownload.stop(mission).subscribe()
    }

    private fun install() {
        RxDownload.extension(mission, ApkInstallExtension::class.java).subscribe()
    }

    private fun open() {
        //TODO: open app
    }


    companion object {
        private val TAG = "BasicDownloadActivity"

        private val iconUrl = "http://p5.qhimg.com/dr/72__/t01a362a049573708ae.png"
        private val url = "http://shouji.360tpcdn.com/170922/9ffde35adefc28d3740d4e16612f078a/com.tencent.tmgp.sgame_22011304.apk"
    }

}
