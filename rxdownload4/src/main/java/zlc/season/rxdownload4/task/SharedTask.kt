package zlc.season.rxdownload4.task

import io.reactivex.disposables.Disposable
import io.reactivex.flowables.ConnectableFlowable
import zlc.season.rxdownload4.Progress

class SharedTask(
        val task: Task,
        val connectableFlowable: ConnectableFlowable<Progress>,
        var disposable: Disposable? = null
)