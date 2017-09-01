package zlc.season.rxdownload3.helper

import io.reactivex.disposables.Disposable


fun dispose(disposable: Disposable?) {
    if (disposable != null && !disposable.isDisposed) {
        disposable.dispose()
    }
}