package zlc.season.rxdownload.kotlin_demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import kotlinx.android.synthetic.main.activity_basic_download.*
import kotlinx.android.synthetic.main.content_basic_download.*
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.log

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
//        disposable = RxDownload.create(url, autoStart = true)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { status ->
//                    currentStatus = status
//                    setProgress(status)
//                    setActionText(status)
//                }
        url.download()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposableSubscriber<zlc.season.rxdownload4.Status>() {
//                    override fun onStart() {
//                        request(1)
//                    }

                    override fun onComplete() {

                    }

                    override fun onNext(t: zlc.season.rxdownload4.Status) {
//                        request(5)
//                        t.log()
                        "onnext".log()
                    }

                    override fun onError(t: Throwable?) {
                        t?.log()
                    }
                })


//                subscribe({
//
//                    Log.d("TAG",it.toString())
//                    progress.max = it.totalSize.toInt()
//                    progress.progress = it.downloadSize.toInt()
//
//                    percent.text = it.percent()
////                    size.text = status.formatString()
//                }, {
//                    Log.w("TAG", it)
//                })
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
    }

}