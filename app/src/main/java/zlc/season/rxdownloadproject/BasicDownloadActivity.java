package zlc.season.rxdownloadproject;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import zlc.season.rxdownload3.RxDownload;
import zlc.season.rxdownload3.core.Status;
import zlc.season.rxdownloadproject.databinding.ActivityBasicDownloadBinding;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static zlc.season.rxdownload3.helper.UtilsKt.dispose;

public class BasicDownloadActivity extends AppCompatActivity {
    private static final String TAG = "BasicDownloadActivity";

    private static final String iconUrl = "http://pp.myapp.com/ma_icon/0/icon_6633_1505724536/256";
    private static final String url = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";

    private ActivityBasicDownloadBinding binding;
    private Disposable disposable;
    private Status currentStatus = new Status().toSuspend();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission(WRITE_EXTERNAL_STORAGE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_basic_download);
        setSupportActionBar(binding.toolbar);

        Picasso.with(this).load(iconUrl).into(binding.contentBasicDownload.img);

        setAction();
    }

    private void setAction() {
        binding.contentBasicDownload.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Status.Companion.isDownloading(currentStatus)) {
                    stop();
                } else {
                    start();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        create();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dispose(disposable);
    }

    private void start() {
        RxDownload.INSTANCE.start(url).subscribe();
    }

    private void stop() {
        RxDownload.INSTANCE.stop(url).subscribe();
    }

    private void create() {
        disposable = RxDownload.INSTANCE.create(url)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Status>() {
                    @Override
                    public void accept(Status status) throws Exception {
                        currentStatus = status;

                        System.out.println(status.getDownloadSize() + " " + status.getTotalSize());

                        binding.contentBasicDownload.progress.setMax((int) status.getTotalSize());
                        binding.contentBasicDownload.progress.setProgress((int) status.getDownloadSize());

                        binding.contentBasicDownload.percent.setText(status.percent());
                        binding.contentBasicDownload.size.setText(status.formatString());

                        if (Status.Companion.isSuspend(status)) {
                            binding.contentBasicDownload.action.setText("开始");
                        }

                        if (Status.Companion.isWaiting(status)) {
                            binding.contentBasicDownload.action.setText("等待中");
                        }

                        if (Status.Companion.isDownloading(status)) {
                            binding.contentBasicDownload.action.setText("暂停");
                        }

                        if (Status.Companion.isFailed(status)) {
                            binding.contentBasicDownload.action.setText("失败");
                        }

                        if (Status.Companion.isSucceed(status)) {
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
