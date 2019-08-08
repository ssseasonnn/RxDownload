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
fun String.download(): Flowable<Status> {
    return Task(this).download()
}

/**
 * Returns a Download Flowable represent the current task.
 */
fun Task.download(): Flowable<Status> {
    return Request().get(url, header)
            .flatMap {
                mapper.map(it).download(this, it)
            }
}

/**
 * Returns a Shared Download Flowable represent the current url.
 *
 * A Shared Download Flowable means it can be received by multiple downstream.
 */
fun String.share(): Flowable<Status> {
    return Task(this).share()
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
fun Task.share(): Flowable<Status> {
    return get().flowable
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
private fun Task.get(): TaskInfo {
    val taskInfo = TaskPool.get(this)
    return if (taskInfo != null) {
        if (taskInfo.disposable.isDisposed) {
            TaskPool.add(this, createTaskInfo())
        } else {
            taskInfo
        }
    } else {
        TaskPool.add(this, createTaskInfo())
    }
}

private fun Task.createTaskInfo(): TaskInfo {
    val flowable = download().replay(1)
            .also { it.subscribeBy() }
    val disposable = flowable.connect()
    return TaskInfo(flowable, disposable)
}