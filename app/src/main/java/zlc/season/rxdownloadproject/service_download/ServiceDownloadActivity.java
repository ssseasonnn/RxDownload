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
import zlc.season.rxdownload.DownloadRecord;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownloadproject.DownloadStateContext;
import zlc.season.rxdownloadproject.R;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class ServiceDownloadActivity extends AppCompatActivity {
    final String saveName = "王者荣耀.apk";
    final String defaultPath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
    final String url = "http://120.192.69.163/dlied5.myapp.com/myapp/1104466820/1104466820/sgame/10024163_com.tencent" +
            ".tmgp.sgame_u131_1.15.2.13.apk";

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

    private DownloadStateContext mStateContext;

    @OnClick(R.id.action)
    public void onClick() {
        mStateContext.performClick(new DownloadStateContext.Callback() {
            @Override
            public void startDownload() {
                start();
            }

            @Override
            public void pauseDownload() {
                pause();
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
        mStatusText.setText("开始");

        mRxDownload = RxDownload.getInstance().context(this);
        mSubscriptions = new CompositeSubscription();

        mStateContext = new DownloadStateContext(mStatusText, mAction);
        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消订阅,不会暂停下载
        mSubscriptions.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 读取下载状态, 如果存在下载记录,则初始化为上次下载的状态
        Subscription query = mRxDownload.getDownloadRecord(url)
                .subscribe(new Action1<DownloadRecord>() {
                    @Override
                    public void call(DownloadRecord record) {
                        //如果有下载记录才会执行到这里, 如果没有下载记录不会执行这里
                        mProgress.setIndeterminate(record.getStatus().isChunked);
                        mProgress.setMax((int) record.getStatus().getTotalSize());
                        mProgress.setProgress((int) record.getStatus().getDownloadSize());
                        mPercent.setText(record.getStatus().getPercent());
                        mSize.setText(record.getStatus().getFormatStatusString());

                        int flag = record.getDownloadFlag();
                        //设置下载状态
                        mStateContext.setStateAndDisplay(flag);
                    }
                });

        //注册广播接收器, 用于接收下载进度
        Subscription temp = mRxDownload.registerReceiver(url)
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_COMPLETED);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_FAILED);
                    }

                    @Override
                    public void onNext(final DownloadStatus status) {
                        mProgress.setIndeterminate(status.isChunked);
                        mProgress.setMax((int) status.getTotalSize());
                        mProgress.setProgress((int) status.getDownloadSize());
                        mPercent.setText(status.getPercent());
                        mSize.setText(status.getFormatStatusString());
                    }
                });

        //将subscription收集起来,在Activity销毁的时候取消订阅,以免内存泄漏
        mSubscriptions.add(temp);
        mSubscriptions.add(query);
    }

    private void installApk() {
        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_INSTALL);
        Uri uri = Uri.fromFile(new File(defaultPath + File.separator + saveName));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(intent);
    }

    private void start() {
        //开始下载, 先检查权限
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
                .compose(mRxDownload.transformServiceNoReceiver(url, saveName, defaultPath))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_STARTED);
                    }
                });
        mSubscriptions.add(temp);
    }

    /**
     * 暂停下载
     */
    private void pause() {
        Subscription subscription = mRxDownload.pauseServiceDownload(url)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_PAUSED);
                    }
                });
        mSubscriptions.add(subscription);
    }

    /**
     * 取消下载
     */
    private void cancel() {
        Subscription subscription = mRxDownload.cancelServiceDownload(url)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_CANCELED);
                    }
                });
        mSubscriptions.add(subscription);
    }
}
