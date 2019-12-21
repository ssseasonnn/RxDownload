package zlc.season.rxdownload.demo.basic

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_basic_download.*
import kotlinx.android.synthetic.main.common_title.*
import zlc.season.rxdownload.demo.R
import zlc.season.rxdownload.demo.utils.click
import zlc.season.rxdownload.demo.utils.installApk
import zlc.season.rxdownload.demo.utils.load
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.file
import zlc.season.rxdownload4.utils.safeDispose

class DemoActivity : AppCompatActivity() {

    private var disposable: Disposable? = null

    private var state = NORMAL

    companion object {
        const val iconUrl = "http://pp.myapp.com/ma_icon/0/icon_10910_1564113626/256"
        const val url = "https://dldir1.qq.com/weixin/android/weixin706android1460.apk"
//        const val url = "http://dlied5.myapp.com/myapp/1104466820/cos.release-40109/10006654_com.tencent.tmgp.sgame_u367648_1.51.1.23_zmvjbg.apk"

        const val NORMAL = 0
        const val STARTED = 1
        const val PAUSED = 2
        const val COMPLETED = 3
        const val FAILED = 4
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_download)

        back.click { finish() }

        icon.load(iconUrl)

        tv_name.text = "微信"
        tv_size.text = getString(R.string.wechat_desc)
        tv_size.movementMethod = ScrollingMovementMethod()

        button.click { onClick() }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.safeDispose()
    }

    private fun onClick() {
        when (state) {
            NORMAL -> start()
            STARTED -> stop()
            PAUSED -> start()
            COMPLETED -> install()
            FAILED -> start()
        }
    }


    private fun start() {
        disposable = url.download()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = {
                            button.text = "${it.downloadSizeStr()}/${it.totalSizeStr()}"
                            button.setProgress(it)
                        },
                        onComplete = {
                            state = COMPLETED
                            button.text = "安装"
                        },
                        onError = {
                            state = FAILED
                            button.text = "重试"
                        }
                )
        state = STARTED
        button.text = "下载中..."
    }

    private fun stop() {
        state = PAUSED
        disposable.safeDispose()
        button.text = "继续"
    }

    private fun install() {
        val file = url.file()
        if (file.exists()) {
            installApk(file)
        }
    }

}