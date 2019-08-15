package zlc.season.rxdownload.kotlin_demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_basic_download.*
import kotlinx.android.synthetic.main.content_basic_download.*
import kotlinx.android.synthetic.main.item_download.view.*
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.utils.log
import zlc.season.rxdownload4.utils.safeDispose

class BasicDownloadActivity : AppCompatActivity() {

    private var disposable1: Disposable? = null
    private var disposable2: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_download)
        setSupportActionBar(toolbar)

        RxJavaPlugins.setErrorHandler {
            it.log()
        }

        finish.setOnClickListener {
            finish()
        }

        Picasso.with(this).load(iconUrl).into(simple_download_layout.img)
        Picasso.with(this).load(iconUrl).into(share_download_layout.img)

        simple_download_layout.action.setOnClickListener {
            disposable1 = url1.download()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onNext = {
                                simple_download_layout.progress.max = it.totalSize.toInt()
                                simple_download_layout.progress.progress = it.downloadSize.toInt()

                                simple_download_layout.percent.text = it.percentStr()
                            },
                            onError = {
                                it.log()
                            },
                            onComplete = {
                            }
                    )
        }

        share_download_layout.action.setOnClickListener {
            disposable2 = url2.download()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onNext = {
                                share_download_layout.progress.max = it.totalSize.toInt()
                                share_download_layout.progress.progress = it.downloadSize.toInt()

                                share_download_layout.percent.text = it.percentStr()
                            },
                            onError = {
                            },
                            onComplete = {

                            }
                    )
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