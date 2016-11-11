package zlc.season.rxdownloadproject;

import android.Manifest;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;

public class BasicDownloadActivity extends AppCompatActivity {

    public static final int START = 0;
    public static final int PAUSE = 1;
    public static final int DONE = 2;

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
    Button mStatus;
    @BindView(R.id.finish)
    Button mFinish;
    @BindView(R.id.content_basic_download)
    RelativeLayout mContentBasicDownload;
    @BindView(R.id.fab)
    FloatingActionButton mFab;

    private int downloadStatus = START;

    private Subscription subscription;

    @OnClick(R.id.status)
    public void onClick() {
        if (downloadStatus == START) {
            downloadStatus = PAUSE;
            mStatus.setText("暂停");
            startDownload();
        } else if (downloadStatus == PAUSE) {
            BasicDownloadActivity.this.unSubscribe(subscription);
            downloadStatus = START;
            mStatus.setText("继续");
        }
    }

    @OnClick(R.id.finish)
    public void onClickFinsh() {
        BasicDownloadActivity.this.finish();
    }

    void unSubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_download);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        String icon = "http://static.yingyonghui.com/icon/128/4200197.png";
        Picasso.with(this).load(icon).into(mImg);
        mStatus.setText("开始");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unSubscribe(subscription);
    }

    private void startDownload() {
        String url = "http://a.gdown.baidu.com/data/wisegame/f4314d752861cf51/WeChat_900.apk";
        subscription = RxPermissions.getInstance(this)
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
                .compose(RxDownload.getInstance().transform(url, "weixin.apk", null))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        downloadStatus = DONE;
                        mStatus.setText("已完成");
                        BasicDownloadActivity.this.unSubscribe(subscription);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        downloadStatus = START;
                        mStatus.setText("继续");
                        BasicDownloadActivity.this.unSubscribe(subscription);
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
    }
}
