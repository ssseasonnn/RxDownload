package zlc.season.rxdownloadproject.service_download;

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
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownloadproject.R;
import zlc.season.rxdownloadproject.State;

public class ServiceDownloadActivity extends AppCompatActivity {
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
    Button mStatus;

    private int downloadStatus = State.START.getValue();
    private RxDownload mRxDownload;

    private CompositeSubscription mSubscriptions;

    @OnClick(R.id.status)
    public void onClick() {
        if (downloadStatus == State.START.getValue()) {
            downloadStatus = State.PAUSE.getValue();
            mStatus.setText("暂停");
            startDownload();
        } else if (downloadStatus == State.PAUSE.getValue()) {
            downloadStatus = State.START.getValue();
            mStatus.setText("继续");
            /**
             * 暂停下载
             */
            mRxDownload.pauseServiceDownload(url);
        }
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
        mStatus.setText("开始");

        mRxDownload = RxDownload.getInstance().context(this);
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * 取消订阅,不会暂停下载
         */
        mSubscriptions.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * 读取下载状态
         */
        Subscription query = mRxDownload.getDownloadStatus(url)
                .subscribe(new Action1<DownloadStatus>() {
                    @Override
                    public void call(DownloadStatus status) {
                        mProgress.setIndeterminate(status.isChunked);
                        mProgress.setMax((int) status.getTotalSize());
                        mProgress.setProgress((int) status.getDownloadSize());
                        mPercent.setText(status.getPercent());
                        mSize.setText(status.getFormatStatusString());
                    }
                });

        /**
         * 注册广播接收器, 用于接收下载进度
         */
        Subscription temp = mRxDownload.registerReceiver(url).subscribe(new CustomSubscriber());

        mSubscriptions.add(temp);
        mSubscriptions.add(query);
    }

    private void startDownload() {
        Subscription temp = mRxDownload.downloadThroughService(url, "王者荣耀.apk", null, "", "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CustomSubscriber());
        mSubscriptions.add(temp);
    }

    class CustomSubscriber extends Subscriber<DownloadStatus> {

        @Override
        public void onCompleted() {
            downloadStatus = State.DONE.getValue();
            mStatus.setText("已完成");
        }

        @Override
        public void onError(Throwable e) {
            Log.w("TAG", e);
            downloadStatus = State.START.getValue();
            mStatus.setText("继续");
        }

        @Override
        public void onNext(final DownloadStatus status) {
            mProgress.setIndeterminate(status.isChunked);
            mProgress.setMax((int) status.getTotalSize());
            mProgress.setProgress((int) status.getDownloadSize());
            mPercent.setText(status.getPercent());
            mSize.setText(status.getFormatStatusString());
        }
    }
}
