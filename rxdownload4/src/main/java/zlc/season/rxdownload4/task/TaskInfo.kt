package zlc.season.rxdownload4.task

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import zlc.season.rxdownload4.Status

class TaskInfo(
        val flowable: Flowable<Status>,
        val disposable: Disposable) {
}