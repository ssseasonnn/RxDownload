package zlc.season.rxdownload.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_demo_list.*
import kotlinx.android.synthetic.main.demo_list_item.*
import zlc.season.rxdownload.demo.DemoListActivity.DemoListItem.Companion.COMPLETED
import zlc.season.rxdownload.demo.DemoListActivity.DemoListItem.Companion.FAILED
import zlc.season.rxdownload.demo.DemoListActivity.DemoListItem.Companion.NORMAL
import zlc.season.rxdownload.demo.DemoListActivity.DemoListItem.Companion.PAUSED
import zlc.season.rxdownload.demo.DemoListActivity.DemoListItem.Companion.STARTED
import zlc.season.rxdownload.demo.utils.ProgressDrawable
import zlc.season.rxdownload.demo.utils.background
import zlc.season.rxdownload.demo.utils.click
import zlc.season.rxdownload.demo.utils.load
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.utils.safeDispose
import zlc.season.yasha.YashaDataSource
import zlc.season.yasha.YashaItem
import zlc.season.yasha.YashaScope
import zlc.season.yasha.linear

class DemoListActivity : AppCompatActivity() {

    private val dataSource by lazy { DemoListDataSource() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_list)

        recycler_view.linear(dataSource) {

            renderItem<DemoListItem> {
                res(R.layout.demo_list_item)

                onBind {
                    tv_name.text = data.name
                    iv_icon.load(data.icon)
                    btn_action.text = data.stateStr()

                    val progressDrawable = ProgressDrawable()
                    btn_action.background(progressDrawable)

                    btn_action.click {
                        when (data.state) {
                            NORMAL -> {
                                start(progressDrawable)
                            }
                            STARTED -> {
                                stop()
                            }
                            PAUSED -> {
                                start(progressDrawable)
                            }
                            COMPLETED -> {

                            }
                            FAILED -> {
                                start(progressDrawable)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun YashaScope<DemoListItem>.start(progressDrawable: ProgressDrawable) {
        data.disposable = data.flowable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = {
                            progressDrawable.setProgress(it.downloadSize, it.totalSize)
                        },
                        onComplete = {
                            data.state = COMPLETED
                            btn_action.text = data.stateStr()
                        },
                        onError = {
                            data.state = FAILED
                            btn_action.text = data.stateStr()
                        }
                )
        data.state = STARTED
        btn_action.text = data.stateStr()
    }

    private fun YashaScope<DemoListItem>.stop() {
        data.disposable.safeDispose()
        data.state = PAUSED
        btn_action.text = data.stateStr()
    }


    class DemoListItem(
            val name: String,
            val icon: String,
            val url: String,
            val flowable: Flowable<Progress>,
            var disposable: Disposable? = null,
            var state: Int = NORMAL
    ) : YashaItem {
        companion object {
            const val NORMAL = 0
            const val STARTED = 1
            const val PAUSED = 2
            const val COMPLETED = 3
            const val FAILED = 4
        }

        fun stateStr(): String {
            return when (state) {
                NORMAL -> "下载"
                STARTED -> "暂停"
                PAUSED -> "继续"
                COMPLETED -> "安装"
                FAILED -> "重试"
                else -> "NONE"
            }
        }

        fun action(): () -> Unit {
            return when (state) {
                NORMAL -> {
                    { url.download() }
                }
                STARTED -> {
                    {}
                }
                PAUSED -> {
                    {}
                }
                COMPLETED -> {
                    {}
                }
                FAILED -> {
                    {}
                }
                else -> {
                    {}
                }
            }
        }
    }

    class DemoListDataSource : YashaDataSource() {
        val iconUrl = "http://pp.myapp.com/ma_icon/0/icon_10910_1564113626/256"
        val url = "https://dldir1.qq.com/weixin/android/weixin706android1460.apk"

        override fun loadInitial(loadCallback: LoadCallback<YashaItem>) {
            val testData = mutableListOf<DemoListItem>()
            val a = DemoListItem("微信", iconUrl, url, url.download())
            testData.add(a)

            loadCallback.setResult(testData)
        }

        override fun loadAfter(loadCallback: LoadCallback<YashaItem>) {
            loadCallback.setResult(emptyList())
        }
    }
}
