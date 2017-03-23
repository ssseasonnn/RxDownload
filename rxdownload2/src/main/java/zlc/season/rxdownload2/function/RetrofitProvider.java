package zlc.season.rxdownload2.function;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import zlc.season.rxdownload2.BuildConfig;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/19
 * Time: 10:16
 * <p>
 * 提供一个默认的,线程安全的Retrofit单例
 */
public class RetrofitProvider {

    private static String ENDPOINT = "http://example.com/api/";

    private RetrofitProvider() {
    }

    /**
     * 指定endpoint
     *
     * @param endpoint endPoint
     * @return Retrofit
     */
    public static Retrofit getInstance(String endpoint) {
        ENDPOINT = endpoint;
        return SingletonHolder.INSTANCE;
    }

    /**
     * 不指定endPoint
     *
     * @return Retrofit
     */
    public static Retrofit getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final Retrofit INSTANCE = create();

        private static Retrofit create() {
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            builder.readTimeout(10, TimeUnit.SECONDS);
            builder.connectTimeout(9, TimeUnit.SECONDS);

            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(interceptor);
            }

            return new Retrofit.Builder().baseUrl(ENDPOINT)
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        }
    }
}
