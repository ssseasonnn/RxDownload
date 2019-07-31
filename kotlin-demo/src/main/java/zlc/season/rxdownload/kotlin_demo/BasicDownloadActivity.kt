package zlc.season.rxdownload.kotlin_demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_basic_download.*
import kotlinx.android.synthetic.main.content_basic_download.*
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.utils.log

class BasicDownloadActivity : AppCompatActivity() {

    private var disposable: Disposable? = null
    private var currentStatus = Status()

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
                is Succeed -> install()
                is ApkInstallExtension.Installed -> open()
            }
        }

        finish.setOnClickListener { finish() }
    }

    @SuppressLint("CheckResult")
    private fun create() {

//        val mission = Mission(url, "test.apk", DEFAULT_SAVE_PATH, false, url, true)
//        disposable = RxDownload.create(mission, autoStart = true)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { status ->
//                    currentStatus = status
//                    setProgress(status)
//                    setActionText(status)
//                }

        url1.download()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(Looper.getMainLooper(), true))
                .subscribeBy(
                        onNext = {
                            progress_new.max = it.totalSize.toInt()
                            progress_new.progress = it.downloadSize.toInt()

                            percent_new.text = it.percent()
                            it.log()
                        },
                        onError = {
                            it.log()
                        },
                        onComplete = {
                            "complete".log()
                        }
                )
    }

    private fun setProgress(status: Status) {
        progress.max = status.totalSize.toInt()
        progress.progress = status.downloadSize.toInt()

        percent.text = status.percent()
        size.text = status.formatString()
    }

    private fun setActionText(status: Status) {
        val text = when (status) {
            is Normal -> "开始"
            is Suspend -> "已暂停"
            is Waiting -> "等待中"
            is Downloading -> "暂停"
            is Failed -> "失败"
            is Succeed -> "安装"
            is ApkInstallExtension.Installing -> "安装中"
            is ApkInstallExtension.Installed -> "打开"
            else -> ""
        }
        action.text = text
    }

    private fun start() {
        RxDownload.start(url).subscribe()
    }

    private fun stop() {
        RxDownload.stop(url).subscribe()
    }

    private fun install() {
        RxDownload.extension(url, ApkInstallExtension::class.java).subscribe()
    }

    private fun open() {
        //TODO: open app
    }


    companion object {
        private val TAG = "BasicDownloadActivity"

        private val iconUrl = "http://pp.myapp.com/ma_icon/0/icon_10910_1564113626/256"
        private val url = "https://dldir1.qq.com/weixin/android/weixin706android1460.apk"
        private val url1 = "http://113.137.62.148/imtt.dd.qq.com/16891/C5801BF68962B59C75CCD573F01AFED1.apk"
    }

}