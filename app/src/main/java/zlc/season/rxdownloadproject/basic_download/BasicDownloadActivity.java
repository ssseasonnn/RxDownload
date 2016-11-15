package zlc.season.rxdownloadproject.basic_download;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import zlc.season.rxdownload.DownloadRecord;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownloadproject.DownloadStateContext;
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

    private Subscription subscription;
    private DownloadStateContext mStateContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_download);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        String icon = "http://static.yingyonghui.com/icon/128/4200197.png";
        Picasso.with(this).load(icon).into(mImg);
        mStatus.setText("开始");

        mStateContext = new DownloadStateContext(mStatus, mAction);
        mStateContext.setState(DownloadRecord.FLAG_NORMAL);
        mStateContext.displayNowState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unSubscribe(subscription);
    }

    @OnClick({R.id.action, R.id.finish})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action:
                mStateContext.nextState(new DownloadStateContext.Callback() {
                    @Override
                    public void startDownload() {
                        start();
                    }

                    @Override
                    public void cancelDownload() {

                    }

                    @Override
                    public void pauseDownload() {
                        pause();
                    }
                });
                break;
            case R.id.finish:
                BasicDownloadActivity.this.finish();
                break;
        }
    }

    void unSubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    private void start() {
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
                        mStateContext.setState(DownloadRecord.FLAG_COMPLETED);
                        mStateContext.displayNowState();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mStateContext.setState(DownloadRecord.FLAG_FAILED);
                        mStateContext.displayNowState();
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

    private void pause() {
        BasicDownloadActivity.this.unSubscribe(subscription);
    }
}
