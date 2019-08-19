package zlc.season.rxdownload4.management

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import zlc.season.rxdownload4.Progress

class SharedTask(
        val flowable: Flowable<Progress>,
        val disposable: Disposable) {
}