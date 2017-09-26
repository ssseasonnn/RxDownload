package zlc.season.rxdownload.kotlin_demo

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityBasicDownloadBinding
import zlc.season.rxdownload.kotlin_demo.databinding.ContentBasicDownloadBinding
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.helper.dispose

class BasicDownloadActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityBasicDownloadBinding
    private lateinit var contentBinding: ContentBasicDownloadBinding

    private var disposable: Disposable? = null
    private var currentStatus = Status()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission(WRITE_EXTERNAL_STORAGE)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_basic_download)
        contentBinding = mainBinding.contentBasicDownload!!

        setSupportActionBar(mainBinding.toolbar)

        loadImg()
        setAction()
        create()
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose(disposable)
    }

    private fun loadImg() {
        Picasso.with(this).load(iconUrl).into(contentBinding.img)
    }

    private fun setAction() {
        contentBinding.action.setOnClickListener {
            when (currentStatus) {
                is Suspend -> start()
                is Failed -> start()
                is Downloading -> stop()
                is Succeed -> install()
                is ApkInstallExtension.Installed -> open()
            }
        }
    }

    private fun create() {
        disposable = RxDownload.create(url)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    currentStatus = status

                    setProgress(status)
                    setActionText(status)
                }
    }

    private fun setProgress(status: Status) {
        contentBinding.progress.max = status.totalSize.toInt()
        contentBinding.progress.progress = status.downloadSize.toInt()

        contentBinding.percent.text = status.percent()
        contentBinding.size.text = status.formatString()
    }

    private fun setActionText(status: Status) {
        val text = when (status) {
            is Suspend -> "开始"
            is Waiting -> "等待中"
            is Downloading -> "暂停"
            is Failed -> "失败"
            is Succeed -> "安装"
            is ApkInstallExtension.Installing -> "安装中"
            is ApkInstallExtension.Installed -> "打开"
            else -> ""
        }
        contentBinding.action.text = text
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

    private fun requestPermission(permission: String) {
        RxPermissions(this)
                .request(permission)
                .subscribe({
                    if (!it) {
                        finish()
                    }
                })
    }

    companion object {
        private val TAG = "BasicDownloadActivity"

        private val iconUrl = "http://pp.myapp.com/ma_icon/0/icon_6633_1505724536/256"
        private val url = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk"
    }

}
