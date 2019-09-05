package zlc.season.rxdownload.demo.manager

import android.annotation.SuppressLint
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.rxdownload4.recorder.TaskEntity
import zlc.season.rxdownload4.recorder.getTaskPage
import zlc.season.yasha.YashaDataSource
import zlc.season.yasha.YashaItem

class DemoManagerDataSource : YashaDataSource() {
    private var page = 0
    private val pageSize = 10

    @SuppressLint("CheckResult")
    override fun loadInitial(loadCallback: LoadCallback<YashaItem>) {
        getTaskPage(page, pageSize)
                .map { mapResult(it) }
                .subscribeBy {
                    loadCallback.setResult(it)
                }

    }

    @SuppressLint("CheckResult")
    override fun loadAfter(loadCallback: LoadCallback<YashaItem>) {
        page++

        getTaskPage(page, pageSize)
                .map { mapResult(it) }
                .subscribeBy {
                    loadCallback.setResult(it)
                }
    }

    private fun mapResult(list: List<TaskEntity>): MutableList<DemoManagerItem> {
        val result = mutableListOf<DemoManagerItem>()
        list.mapTo(result) { DemoManagerItem(it.task) }
        return result
    }
}