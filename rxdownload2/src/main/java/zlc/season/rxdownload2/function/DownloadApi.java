package zlc.season.rxdownload2.function;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:02
 * Download Api
 */
public interface DownloadApi {

    @GET
    @Streaming
    Flowable<Response<ResponseBody>> download(@Header("Range") String range,
                                              @Url String url);

    @HEAD
    Observable<Response<Void>> check(@Url String url);

    @HEAD
    Observable<Response<Void>> checkRangeByHead(@Header("Range") String range,
                                                @Url String url);

    @HEAD
    Observable<Response<Void>> checkFileByHead(@Header("If-Modified-Since") String lastModify,
                                               @Url String url);
}
