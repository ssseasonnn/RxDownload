package zlc.season.rxdownload.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_demo_list.*
import kotlinx.android.synthetic.main.demo_list_item.*
import zlc.season.rxdownload.demo.utils.ProgressButton
import zlc.season.rxdownload.demo.utils.click
import zlc.season.rxdownload.demo.utils.load
import zlc.season.rxdownload.demo.utils.mock_json
import zlc.season.rxdownload4.get
import zlc.season.rxdownload4.share
import zlc.season.rxdownload4.start
import zlc.season.rxdownload4.stop
import zlc.season.rxdownload4.task.SharedTask
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.safeDispose
import zlc.season.yasha.YashaDataSource
import zlc.season.yasha.YashaItem
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
                    tv_size.text = data.size

                    iv_icon.load(data.icon)
                    btn_action.text = data.stateStr()

                    data.subscribe(btn_action)

                    btn_action.click {
                        data.action(btn_action)
                    }
                }

                onDetach {
                    data.dispose()
                }

                onAttach {
                    data.subscribe(btn_action)
                }

            }
        }
    }

    class DemoListItem(
            val name: String,
            val icon: String,
            val url: String,
            val size: String,

            var state: Int = NORMAL
    ) : YashaItem {

        //for download use
        var disposable: Disposable? = null

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

        fun action(button: ProgressButton) {
            when (state) {
                NORMAL -> start(button)
                STARTED -> stop(button)
                PAUSED -> start(button)
                COMPLETED -> {
                }
                FAILED -> start(button)
            }
        }

        fun subscribe(button: ProgressButton) {
            disposable = Task(url).share().get()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onNext = {
                                button.setProgress(it.downloadSize, it.totalSize)
                            },
                            onComplete = {
                                state = COMPLETED
                                button.text = stateStr()
                            },
                            onError = {
                                state = FAILED
                                button.text = stateStr()
                            }
                    )
        }

        fun dispose() {
            disposable.safeDispose()
        }

        private fun start(button: ProgressButton) {
            Task(url).share().start()
            state = STARTED
            button.text = stateStr()
        }

        private fun stop(button: ProgressButton) {
            Task(url).share().stop()
            state = PAUSED
            button.text = stateStr()
        }
    }

    class DemoListDataSource : YashaDataSource() {

        override fun loadInitial(loadCallback: LoadCallback<YashaItem>) {
            val type = object : TypeToken<List<DemoListItem>>() {}.type
            val mockData = Gson().fromJson<List<DemoListItem>>(mock_json, type)
            loadCallback.setResult(mockData)
        }

        override fun loadAfter(loadCallback: LoadCallback<YashaItem>) {
            loadCallback.setResult(emptyList())
        }
    }
}
