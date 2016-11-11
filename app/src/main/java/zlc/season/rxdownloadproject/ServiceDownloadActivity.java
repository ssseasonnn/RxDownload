package zlc.season.rxdownloadproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;

public class ServiceDownloadActivity extends AppCompatActivity {
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

    private int downloadStatus = State.START.getValue();
    private Subscription subscription;

    @OnClick(R.id.status)
    public void onClick() {
        if (downloadStatus == State.START.getValue()) {
            downloadStatus = State.PAUSE.getValue();
            mStatus.setText("暂停");
            startDownload();
        } else if (downloadStatus == State.PAUSE.getValue()) {
            ServiceDownloadActivity.this.unSubscribe(subscription);
            downloadStatus = State.START.getValue();
            mStatus.setText("继续");
        }
    }

    @OnClick(R.id.finish)
    public void onClickFinsh() {
        ServiceDownloadActivity.this.finish();
    }

    void unSubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_download);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        String icon = "http://static.yingyonghui.com/icon/128/4196396.png";
        Picasso.with(this).load(icon).into(mImg);
        mStatus.setText("开始");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unSubscribe(subscription);
    }

    private void startDownload() {
        String url = "http://120.192.69.163/dlied5.myapp.com/myapp/1104466820/1104466820/sgame/10024163_com.tencent" +
                ".tmgp.sgame_u131_1.15.2.13.apk";
        subscription = RxDownload.getInstance().downloadWithService(this, url, "王者荣耀.apk", null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        downloadStatus = State.DONE.getValue();
                        mStatus.setText("已完成");
                        ServiceDownloadActivity.this.unSubscribe(subscription);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        downloadStatus = State.START.getValue();
                        mStatus.setText("继续");
                        ServiceDownloadActivity.this.unSubscribe(subscription);
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
