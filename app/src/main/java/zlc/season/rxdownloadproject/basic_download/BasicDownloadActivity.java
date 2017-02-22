package zlc.season.rxdownloadproject.basic_download;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadBean;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownload2.function.Utils;
import zlc.season.rxdownloadproject.DownloadController;
import zlc.season.rxdownloadproject.R;

public class BasicDownloadActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.img)
    ImageView mImg;
    @BindView(R.id.status)
    TextView mStatus;
    @BindView(R.id.percent)
    TextView mPercent;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.size)
    TextView mSize;
    @BindView(R.id.action)
    Button mAction;
    @BindView(R.id.finish)
    Button mFinish;

    private String url = "http://dldir1.qq.com/weixin/android/weixin6330android920.apk";
    private String image = "http://static.yingyonghui.com/icon/128/4200197.png";

    private Disposable disposable;
    private RxDownload rxDownload;
    private DownloadController downloadController;

    @OnClick({R.id.action, R.id.finish})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action:
                downloadController.handleClick(new DownloadController.Callback() {
                    @Override
                    public void startDownload() {
                        start();
                    }

                    @Override
                    public void pauseDownload() {
                        pause();
                    }

                    @Override
                    public void cancelDownload() {
                    }

                    @Override
                    public void install() {
                        installApk();
                    }
                });
                break;
            case R.id.finish:
                BasicDownloadActivity.this.finish();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_download);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        Picasso.with(this).load(image).into(mImg);
        mAction.setText("开始");

        rxDownload = RxDownload.getInstance(this)
                .maxThread(3);
        downloadController = new DownloadController(mStatus, mAction);
        downloadController.setState(new DownloadController.Normal());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.dispose(disposable);
    }

    private void start() {
        RxPermissions.getInstance(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (!aBoolean) {
                            throw new RuntimeException("no permission");
                        }
                    }
                })
                .observeOn(Schedulers.io())
                .compose(rxDownload.<Boolean>transform(new DownloadBean.Builder(url).setExtra1(image).setExtra2("微信").build()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DownloadStatus>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        downloadController.setState(new DownloadController.Started());
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        mProgress.setIndeterminate(status.isChunked);
                        mProgress.setMax((int) status.getTotalSize());
                        mProgress.setProgress((int) status.getDownloadSize());
                        mPercent.setText(status.getPercent());
                        mSize.setText(status.getFormatStatusString());
                    }

                    @Override
                    public void onError(Throwable e) {
                        downloadController.setState(new DownloadController.Paused());
                    }

                    @Override
                    public void onComplete() {
                        downloadController.setState(new DownloadController.Completed());
                    }
                });
    }

    private void pause() {
        downloadController.setState(new DownloadController.Paused());
        Utils.dispose(disposable);
    }

    private void installApk() {
        File[] files = rxDownload.getRealFiles(url);
        if (files != null) {
            Uri uri = Uri.fromFile(files[0]);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            startActivity(intent);
        } else {
            Toast.makeText(this, "File not exists", Toast.LENGTH_SHORT).show();
        }
    }
}
