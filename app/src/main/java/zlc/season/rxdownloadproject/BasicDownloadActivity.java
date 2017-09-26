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
import zlc.season.rxdownload3.core.Downloading;
import zlc.season.rxdownload3.core.Failed;
import zlc.season.rxdownload3.core.Status;
import zlc.season.rxdownload3.core.Succeed;
import zlc.season.rxdownload3.core.Suspend;
import zlc.season.rxdownload3.core.Waiting;
import zlc.season.rxdownload3.extension.ApkInstallExtension;
import zlc.season.rxdownloadproject.databinding.ActivityBasicDownloadBinding;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static zlc.season.rxdownload3.helper.UtilsKt.dispose;

public class BasicDownloadActivity extends AppCompatActivity {
    private static final String TAG = "BasicDownloadActivity";

    private static final String iconUrl = "http://pp.myapp.com/ma_icon/0/icon_6633_1505724536/256";
    //        private static final String url = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";
    private static final String url = "http://imtt.dd.qq.com/16891/76E3D055C6075BE342B562474F2B1AA3.apk?fsname=com.estrongs.android.pop_4.1.6.7.7_589.apk&csr=db5e";

    private ActivityBasicDownloadBinding binding;
    private Disposable disposable;
    private Status currentStatus = new Status();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission(WRITE_EXTERNAL_STORAGE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_basic_download);
        setSupportActionBar(binding.toolbar);

        Picasso.with(this).load(iconUrl).into(binding.contentBasicDownload.img);

        setAction();

        create();
    }

    private void setAction() {
        binding.contentBasicDownload.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentStatus instanceof Failed || currentStatus instanceof Suspend) {
                    start();
                }

                if (currentStatus instanceof Downloading) {
                    stop();
                }

                if (currentStatus instanceof Succeed) {
                    install();
                }

                if (currentStatus instanceof ApkInstallExtension.Installed) {
                    open();
                }
            }
        });
    }

    private void open() {

    }

    private void install() {
        RxDownload.INSTANCE.extension(url, ApkInstallExtension.class).subscribe();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
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

                        binding.contentBasicDownload.progress.setMax((int) status.getTotalSize());
                        binding.contentBasicDownload.progress.setProgress((int) status.getDownloadSize());

                        binding.contentBasicDownload.percent.setText(status.percent());
                        binding.contentBasicDownload.size.setText(status.formatString());

                        if (status instanceof Suspend) {
                            binding.contentBasicDownload.action.setText("开始");
                        }

                        if (status instanceof Waiting) {
                            binding.contentBasicDownload.action.setText("等待中");
                        }

                        if (status instanceof Downloading) {
                            binding.contentBasicDownload.action.setText("暂停");
                        }

                        if (status instanceof Failed) {
                            binding.contentBasicDownload.action.setText("失败");
                        }

                        if (status instanceof Succeed) {
                            binding.contentBasicDownload.action.setText("安装");
                        }

                        if (status instanceof ApkInstallExtension.Installing) {
                            binding.contentBasicDownload.action.setText("安装中");
                        }

                        if (status instanceof ApkInstallExtension.Installed) {
                            binding.contentBasicDownload.action.setText("打开");
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
