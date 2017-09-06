package zlc.season.rxdownloadproject;

import android.Manifest;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import zlc.season.rxdownload3.RxDownload;
import zlc.season.rxdownloadproject.databinding.ActivityBasicDownloadBinding;

import static zlc.season.rxdownload3.helper.DisposableUtilKt.dispose;

public class BasicDownloadActivity extends AppCompatActivity {

    private static final String url = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";

    private ActivityBasicDownloadBinding binding;
    private Disposable disposable;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_basic_download);
        setSupportActionBar(binding.toolbar);

        binding.contentBasicDownload.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RxDownload.INSTANCE.start(url).subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.w("TAG", throwable);
                    }
                });
            }
        });


        createDownloadMission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispose(disposable);
    }

    private void createDownloadMission() {
        disposable = RxDownload.INSTANCE.create(url)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DownloadStatus>() {
                    @Override
                    public void accept(DownloadStatus downloadStatus) throws Exception {
                        binding.contentBasicDownload.progress.setProgress((int) downloadStatus.getDownloadSize());
                        binding.contentBasicDownload.progress.setMax((int) downloadStatus.getTotalSize());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.w("TAG", throwable);
                    }
                });
    }

    private void requestPermission(String permission) {
        RxPermissions.getInstance(this)
                .request(permission)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (!aBoolean) {
                            finish();
                        }
                    }
                });
    }

}
