package zlc.season.rxdownload.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_basic_download.*
import kotlinx.android.synthetic.main.common_title.*
import zlc.season.rxdownload.demo.utils.ProgressDrawable
import zlc.season.rxdownload.demo.utils.click
import zlc.season.rxdownload.demo.utils.installApk
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.file
import zlc.season.rxdownload4.utils.safeDispose

class DemoActivity : AppCompatActivity() {

    private var disposable: Disposable? = null
    private val progressDrawable = ProgressDrawable()

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_download)

        back.click { finish() }

        Picasso.with(this).load(iconUrl).into(icon)

        tv_title.text = "微信"
        tv_desc.text = getString(R.string.wechat_desc)
        tv_desc.movementMethod = ScrollingMovementMethod()

        button.background = progressDrawable

        button.click { start() }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.safeDispose()
    }

    private fun start() {
        disposable = url.download()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = {
                            button.text = "${it.downloadSizeStr()}/${it.totalSizeStr()}"
                            progressDrawable.setProgress(it.downloadSize, it.totalSize)

                            button.click { stop() }
                        },
                        onComplete = {
                            button.text = "安装"
                            button.click { install() }
                        },
                        onError = {
                            button.text = "重试"
                            button.click { start() }
                        }
                )
    }

    private fun stop() {
        disposable.safeDispose()
        button.text = "继续"

        button.click {
            start()
        }
    }

    private fun install() {
        val file = url.file()
        if (file.exists()) {
            installApk(file)
        }
    }

    companion object {
        const val iconUrl = "http://pp.myapp.com/ma_icon/0/icon_10910_1564113626/256"
        const val url = "https://dldir1.qq.com/weixin/android/weixin706android1460.apk"
    }

}