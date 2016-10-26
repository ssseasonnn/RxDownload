package zlc.season.rxdownload;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * add custom header to all http request
 *
 * User: Season(ssseasonnn@gmail.com)
 * Date: 2016-3-25
 * Time: 13:12
 * FIXME
 */
public class HeaderInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
//        Headers headers = new Headers.Builder()
//                .add("key", "value")
//                .add("key", "value")
//                .build();
        Request request = chain.request()
                .newBuilder()
//                .headers(headers)
                .addHeader("Content-Type", "application/json")
                .addHeader("charset", "UTF-8")
                .build();
        return chain.proceed(request);
    }
}
