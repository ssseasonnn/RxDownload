package zlc.season.rxdownload;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:02
 * FIXME
 */
interface DownloadApi {
    @GET
    @Streaming
    Observable<Response<ResponseBody>> download(@Header("Range") String range, @Url String url);

    @HEAD
    Observable<Response<Void>> getHeaders(@Header("Range") String range, @Url String url);

    @HEAD
    Observable<Response<Void>> getHeadersWithLastModify(@Header("If-Modified-Since") Long lastModify,
                                                        @Url String url);

    @HEAD
    Observable<Response<Void>> etag(@Header("If-None-Match") String etag,
                                    @Url String url);
}
