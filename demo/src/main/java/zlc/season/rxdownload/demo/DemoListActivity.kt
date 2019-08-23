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
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.utils.log
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
                    "${data.name} onBind".log()
                    tv_name.text = data.name
                    tv_size.text = data.size

                    iv_icon.load(data.icon)

                    btn_action.click {
                        data.action()
                    }
                }

                onDetach {
                    "${data.name} onDetach".log()
                    data.dispose()
                }

                onAttach {
                    "${data.name} onAttach".log()
                    data.subscribe(btn_action)
                    btn_action.text = data.stateStr()
                }

            }
        }
    }

    class DemoListItem(
            val name: String,
            val icon: String,
            val url: String,
            val size: String
    ) : YashaItem {

        //for download use
        var disposable: Disposable? = null
        var progress: Progress? = null

        fun stateStr(): String {
            return when (Task(url).manager().currentStatus()) {
                is Normal -> "下载"
                is Started -> "暂停"
                is Paused -> "继续"
                is Completed -> "安装"
                is Failed -> "重试"
                is Downloading -> "暂停"
            }
        }

        fun action() {
            val taskManager = Task(url).manager()
            when (taskManager.currentStatus()) {
                is Normal -> taskManager.start()
                is Started -> taskManager.stop()
                is Downloading -> taskManager.stop()
                is Completed -> {
                }
                is Failed -> taskManager.start()
                is Paused -> taskManager.start()
            }
        }

        fun subscribe(btn_action: ProgressButton) {
            val taskManager = Task(url).manager()

            btn_action.setProgress(taskManager.currentProgress().downloadSize,
                    taskManager.currentProgress().totalSize)

            disposable = taskManager.status()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy { status ->
                        if (status is Downloading) {
                            btn_action.setProgress(
                                    status.progress.downloadSize,
                                    status.progress.totalSize
                            )
                        }

                        btn_action.text = stateStr()
                    }
        }

        fun dispose() {
            disposable.safeDispose()
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
