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
import zlc.season.rxdownload3.core.Downloading;
import zlc.season.rxdownload3.core.Empty;
import zlc.season.rxdownload3.core.Failed;
import zlc.season.rxdownload3.core.Status;
import zlc.season.rxdownload3.core.Succeed;
import zlc.season.rxdownload3.core.Waiting;
import zlc.season.rxdownloadproject.databinding.ActivityBasicDownloadBinding;

import static zlc.season.rxdownload3.helper.UtilsKt.dispose;

public class BasicDownloadActivity extends AppCompatActivity {
    private static final String TAG = "BasicDownloadActivity";

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
                        Log.w(TAG, throwable);
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
                .subscribe(new Consumer<Status>() {
                    @Override
                    public void accept(Status status) throws Exception {
                        System.out.println(status.getDownloadSize());
                        System.out.println(status.getTotalSize());
                        binding.contentBasicDownload.percent.setText(status.percent());
                        binding.contentBasicDownload.size.setText(status.formatString());
                        binding.contentBasicDownload.progress.setProgress((int) status.getDownloadSize());
                        binding.contentBasicDownload.progress.setMax((int) status.getTotalSize());

                        if (status instanceof Empty) {
                            binding.contentBasicDownload.action.setText("开始");
                        }

                        if (status instanceof Downloading) {
                            binding.contentBasicDownload.action.setText("暂停");
                        }

                        if (status instanceof Failed) {
                            binding.contentBasicDownload.action.setText("失败");
                            Failed failed = (Failed) status;
                            Log.w(TAG, failed.getThrowable());
                        }

                        if (status instanceof Waiting) {
                            binding.contentBasicDownload.action.setText("等待中");
                        }

                        if (status instanceof Succeed) {
                            binding.contentBasicDownload.action.setText("完成");
                        }
                    }
                });
    }

    private void requestPermission(String permission) {
        new RxPermissions(this)
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
