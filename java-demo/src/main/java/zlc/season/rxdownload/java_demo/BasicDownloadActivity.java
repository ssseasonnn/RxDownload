package zlc.season.rxdownload.java_demo;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.squareup.picasso.Picasso;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import zlc.season.rxdownload.java_demo.databinding.ActivityBasicDownloadBinding;
import zlc.season.rxdownload.java_demo.databinding.ContentBasicDownloadBinding;
import zlc.season.rxdownload3.RxDownload;
import zlc.season.rxdownload3.core.Downloading;
import zlc.season.rxdownload3.core.Failed;
import zlc.season.rxdownload3.core.Normal;
import zlc.season.rxdownload3.core.Status;
import zlc.season.rxdownload3.core.Succeed;
import zlc.season.rxdownload3.core.Suspend;
import zlc.season.rxdownload3.core.Waiting;
import zlc.season.rxdownload3.extension.ApkInstallExtension;

import static zlc.season.rxdownload3.helper.UtilsKt.dispose;

public class BasicDownloadActivity extends AppCompatActivity {
    private static final String iconUrl = "http://p5.qhimg.com/dr/72__/t01a362a049573708ae.png";
    private static final String url = "http://shouji.360tpcdn.com/170922/9ffde35adefc28d3740d4e16612f078a/com.tencent.tmgp.sgame_22011304.apk";

    private ActivityBasicDownloadBinding mainBinding;
    private ContentBasicDownloadBinding contentMainBinding;
    private Disposable disposable;
    private Status currentStatus = new Status();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_basic_download);
        contentMainBinding = mainBinding.contentBasicDownload;
        setSupportActionBar(mainBinding.toolbar);

        Picasso.with(this).load(iconUrl).into(mainBinding.contentBasicDownload.img);
        setAction();
        create();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispose(disposable);
    }

    private void setAction() {
        contentMainBinding.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchClick();
            }
        });

        contentMainBinding.finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void dispatchClick() {
        if (currentStatus instanceof Normal) {
            start();
        } else if (currentStatus instanceof Suspend) {
            start();
        } else if (currentStatus instanceof Failed) {
            start();
        } else if (currentStatus instanceof Downloading) {
            stop();
        } else if (currentStatus instanceof Succeed) {
            install();
        } else if (currentStatus instanceof ApkInstallExtension.Installed) {
            open();
        }
    }

    private void create() {
        disposable = RxDownload.INSTANCE.create(url)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Status>() {
                    @Override
                    public void accept(Status status) throws Exception {
                        currentStatus = status;
                        setProgress(status);
                        setActionText(status);
                    }
                });
    }

    private void setProgress(Status status) {
        contentMainBinding.progress.setMax((int) status.getTotalSize());
        contentMainBinding.progress.setProgress((int) status.getDownloadSize());

        contentMainBinding.percent.setText(status.percent());
        contentMainBinding.size.setText(status.formatString());
    }

    private void setActionText(Status status) {
        String text = "";
        if (status instanceof Normal) {
            text = "开始";
        } else if (status instanceof Suspend) {
            text = "已暂停";
        } else if (status instanceof Waiting) {
            text = "等待中";
        } else if (status instanceof Downloading) {
            text = "暂停";
        } else if (status instanceof Failed) {
            text = "失败";
        } else if (status instanceof Succeed) {
            text = "安装";
        } else if (status instanceof ApkInstallExtension.Installing) {
            text = "安装中";
        } else if (status instanceof ApkInstallExtension.Installed) {
            text = "打开";
        }
        contentMainBinding.action.setText(text);
    }

    private void start() {
        RxDownload.INSTANCE.start(url).subscribe();
    }

    private void stop() {
        RxDownload.INSTANCE.stop(url).subscribe();
    }

    private void install() {
        RxDownload.INSTANCE.extension(url, ApkInstallExtension.class).subscribe();
    }

    private void open() {
        //TODO: open app
    }
}
