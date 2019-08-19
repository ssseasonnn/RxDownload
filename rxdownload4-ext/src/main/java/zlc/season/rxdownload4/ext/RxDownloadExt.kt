package zlc.season.rxdownload4.ext

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.management.SharedTask
import zlc.season.rxdownload4.management.SharedTaskPool


/**
 * Returns a Shared Download Flowable represent the current url.
 *
 * A Shared Download Flowable means it can be received by multiple downstream.
 */
fun String.shareDownload(immediately: Boolean = true): Flowable<Progress> {
    return Task(this).shareDownload(immediately)
}

/**
 * Returns a Disposable represent the Shared Download Flowable.
 *
 * When you call [Disposable.dispose] method, all downstream of the Shared Flowable will stop.
 */
fun String.shareDisposable(): Disposable {
    return Task(this).shareDisposable()
}


/**
 * Returns a Shared Download Flowable represent the current task.
 *
 * A Shared Download Flowable means it can be received by multiple down streams.
 */
fun Task.shareDownload(immediately: Boolean = true): Flowable<Progress> {
    return get(immediately).flowable
}

/**
 * Returns a Disposable represent the Shared Download Flowable.
 *
 * When you call [Disposable.dispose] method, all downstream of the Shared Flowable will stop.
 */
fun Task.shareDisposable(): Disposable {
    return get().disposable
}

@Synchronized
private fun Task.get(immediately: Boolean = true): SharedTask {
    val taskInfo = SharedTaskPool.get(this)
    return if (taskInfo != null) {
        if (taskInfo.disposable.isDisposed) {
            SharedTaskPool.add(this, createTaskInfo(immediately))
        } else {
            taskInfo
        }
    } else {
        SharedTaskPool.add(this, createTaskInfo(immediately))
    }
}

private fun Task.createTaskInfo(immediately: Boolean = true): SharedTask {
    val flowable = download().replay(1)
            .also {
                if (immediately) {
                    it.subscribeBy()
                }
            }
    val disposable = flowable.connect()
    return SharedTask(flowable, disposable)
}