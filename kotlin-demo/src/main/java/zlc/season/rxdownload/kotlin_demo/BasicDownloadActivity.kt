package zlc.season.rxdownload.kotlin_demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_basic_download.*
import zlc.season.rxdownload4.utils.safeDispose
import kotlin.concurrent.thread

class BasicDownloadActivity : AppCompatActivity() {

    private var disposable1: Disposable? = null
    private var disposable2: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_download)

//        Picasso.with(this).load(iconUrl).into(share_download_layout.img)

        val progressDrawable = ProgressDrawable()
        button.setBackgroundDrawable(progressDrawable)

        thread {
            for (i in 0..100) {
                progressDrawable.setProgress(i, 100)
                Thread.sleep(100)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable1.safeDispose()
        disposable2.safeDispose()
    }

    companion object {
        const val iconUrl = "http://pp.myapp.com/ma_icon/0/icon_10910_1564113626/256"

        const val url1 = "https://dldir1.qq.com/weixin/android/weixin706android1460.apk"
        const val url2 = "http://113.137.62.148/imtt.dd.qq.com/16891/C5801BF68962B59C75CCD573F01AFED1.apk"
    }

}