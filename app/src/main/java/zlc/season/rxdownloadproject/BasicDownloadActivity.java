package zlc.season.rxdownloadproject;

import android.Manifest;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import zlc.season.rxdownload3.RxDownload;
import zlc.season.rxdownload3.core.DownloadStatus;
import zlc.season.rxdownloadproject.databinding.ActivityBasicDownloadBinding;

public class BasicDownloadActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBasicDownloadBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_basic_download);
        setSupportActionBar(binding.toolbar);

        binding.contentBasicDownload.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDownload();
            }
        });
    }

    private void startDownload() {
        RxPermissions.getInstance(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .filter(new Predicate<Boolean>() {
                    @Override
                    public boolean test(Boolean aBoolean) throws Exception {
                        return aBoolean;
                    }
                })
                .flatMap(new Function<Boolean, ObservableSource<DownloadStatus>>() {
                    @Override
                    public ObservableSource<DownloadStatus> apply(Boolean aBoolean) throws Exception {
                        return RxDownload.INSTANCE.download("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk");
                    }
                })
                .subscribe(new Consumer<DownloadStatus>() {
                    @Override
                    public void accept(DownloadStatus downloadStatus) throws Exception {
                        System.out.println(downloadStatus);
                    }
                });
    }
}
