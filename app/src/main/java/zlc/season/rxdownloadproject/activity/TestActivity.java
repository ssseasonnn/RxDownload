package zlc.season.rxdownloadproject.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;
import zlc.season.rxdownload2.function.DownloadApi;
import zlc.season.rxdownload2.function.RetrofitProvider;
import zlc.season.rxdownloadproject.R;

import static zlc.season.rxdownload2.function.Utils.log;

/**
 * Created by Ray on 2017/6/17.
 */

public class TestActivity extends AppCompatActivity {


    @BindView(R.id.btnDownload)
    Button btnDownload;

    DownloadApi downloadApi;
    String url = "http://gdown.baidu.com/data/wisegame/e2ec5ae15b93fdaa/anzhuoshichang_16793302.apk";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);
        downloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
    }

    @OnClick(R.id.btnDownload)
    void download() {
        downloadApi.download("bytes=0-3825789", url)
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Response<ResponseBody>>() {
                    @Override
                    public void accept(@NonNull Response<ResponseBody> responseBodyResponse) throws Exception {
                        InputStream inStream = responseBodyResponse.body().byteStream();
                        int readLen;
                        byte[] buffer = new byte[2048];
                        long start = 0;
                        long oldStart = start;

                        while ((readLen = inStream.read(buffer)) != -1) {
                            start += readLen;
                            if (start - oldStart > 100000L) {
                                log("Thread: " + Thread.currentThread().getName() + "; saveLen: " + start);
                                oldStart = start;
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        log(throwable);
                    }
                });
        downloadApi.download("bytes=3825790-7651579", url)
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Response<ResponseBody>>() {
                    @Override
                    public void accept(@NonNull Response<ResponseBody> responseBodyResponse) throws Exception {
                        InputStream inStream = responseBodyResponse.body().byteStream();
                        int readLen;
                        byte[] buffer = new byte[2048];
                        long start = 3825790;
                        long oldStart = start;

                        while ((readLen = inStream.read(buffer)) != -1) {
                            start += readLen;
                            if(start - oldStart > 100000L){
                                log("Thread: " + Thread.currentThread().getName() + "; saveLen: " + start);
                                oldStart = start;
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        log(throwable);
                    }
                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
