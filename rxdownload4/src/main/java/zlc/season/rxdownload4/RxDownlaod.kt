package zlc.season.rxdownload4

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.task.Task
import zlc.season.rxdownload4.task.TaskInfo
import zlc.season.rxdownload4.task.TaskPool


/**
 * Returns a Download Flowable represent the current url.
 */
fun String.download(): Flowable<Progress> {
    return Task(this).download()
}

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
 * Returns a Download Flowable represent the current task.
 */
fun Task.download(): Flowable<Progress> {
    return Request().get(url, header)
            .flatMap {
                mapper.map(it).download(this, it)
            }
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
private fun Task.get(immediately: Boolean = true): TaskInfo {
    val taskInfo = TaskPool.get(this)
    return if (taskInfo != null) {
        if (taskInfo.disposable.isDisposed) {
            TaskPool.add(this, createTaskInfo(immediately))
        } else {
            taskInfo
        }
    } else {
        TaskPool.add(this, createTaskInfo(immediately))
    }
}

private fun Task.createTaskInfo(immediately: Boolean = true): TaskInfo {
    val flowable = download().replay(1)
            .also {
                if (immediately) {
                    it.subscribeBy()
                }
            }
    val disposable = flowable.connect()
    return TaskInfo(flowable, disposable)
}