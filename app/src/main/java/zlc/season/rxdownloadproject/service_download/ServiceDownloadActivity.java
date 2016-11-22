package zlc.season.rxdownloadproject.service_download;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownload.entity.DownloadEvent;
import zlc.season.rxdownloadproject.DownloadController;
import zlc.season.rxdownloadproject.R;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class ServiceDownloadActivity extends AppCompatActivity {
    final String saveName = "梦幻西游.apk";
    final String defaultPath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
    final String url = "http://downali.game.uc.cn/wm/6/6/MY-1.98.0_uc_platform2_3306918_082452919a00.apk";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.img)
    ImageView mImg;
    @BindView(R.id.percent)
    TextView mPercent;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.size)
    TextView mSize;
    @BindView(R.id.status)
    TextView mStatusText;
    @BindView(R.id.action)
    Button mAction;

    private RxDownload mRxDownload;
    private CompositeSubscription mSubscriptions;

    private DownloadController mDownloadController;

    @OnClick(R.id.action)
    public void onClick() {
        mDownloadController.handleClick(new DownloadController.Callback() {
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
                cancel();
            }

            @Override
            public void install() {
                installApk();
            }
        });
    }

    @OnClick(R.id.finish)
    public void onClickFinish() {
        ServiceDownloadActivity.this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_download);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        String icon = "http://static.yingyonghui.com/icon/128/4196396.png";
        Picasso.with(this).load(icon).into(mImg);
        mAction.setText("开始");

        mRxDownload = RxDownload.getInstance().context(this);
        mSubscriptions = new CompositeSubscription();

        mDownloadController = new DownloadController();
        mDownloadController.setState(new DownloadController.Normal());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscriptions.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //接收下载进度
        Subscription temp = mRxDownload.receiveDownloadStatus(url)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadEvent>() {
                    @Override
                    public void onCompleted() {
                        mDownloadController.setState(new DownloadController.Completed());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mDownloadController.setState(new DownloadController.Failed());
                    }

                    @Override
                    public void onNext(final DownloadEvent event) {
                        if (event instanceof DownloadEvent.StartedEvent) {
                            mProgress.setIndeterminate(event.downloadStatus.isChunked);
                            mProgress.setMax((int) event.downloadStatus.getTotalSize());
                            mProgress.setProgress((int) event.downloadStatus.getDownloadSize());
                            mPercent.setText(event.downloadStatus.getPercent());
                            mSize.setText(event.downloadStatus.getFormatStatusString());
                        } else if (event instanceof DownloadEvent.PausedEvent) {
                            mAction.setText("继续");
                            mStatusText.setText("已暂停");
                        }
                    }
                });

        mSubscriptions.add(temp);
    }

    private void installApk() {
        Uri uri = Uri.fromFile(new File(defaultPath + File.separator + saveName));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(intent);
    }

    private void start() {
        Subscription temp = RxPermissions.getInstance(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (!granted) {
                            throw new RuntimeException("no permission");
                        }
                    }
                })
                .observeOn(Schedulers.io())
                .compose(mRxDownload.transformServiceWithoutStatus(url, saveName, defaultPath))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mAction.setText("暂停");
                        mStatusText.setText("下载中...");
                        mDownloadController.setState(new DownloadController.Started());
                    }
                });
        mSubscriptions.add(temp);
    }


    private void pause() {
        Subscription subscription = mRxDownload.pauseServiceDownload(url)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                    }
                });
        mSubscriptions.add(subscription);
    }


    private void cancel() {
        Subscription subscription = mRxDownload.cancelServiceDownload(url)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                    }
                });
        mSubscriptions.add(subscription);
    }
}
