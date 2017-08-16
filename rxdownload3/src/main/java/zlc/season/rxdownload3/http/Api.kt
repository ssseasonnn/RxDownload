package zlc.season.rxdownload3.http

import io.reactivex.Observable


interface Api {
    fun <T> check(url: String): Observable<ResponseWrap<T>>
}