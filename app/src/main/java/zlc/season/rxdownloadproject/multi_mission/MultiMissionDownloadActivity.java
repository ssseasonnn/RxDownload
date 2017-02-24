package zlc.season.rxdownloadproject.multi_mission;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;
import online.osslab.CircleProgressBar;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownloadproject.R;

import static zlc.season.rxdownload2.function.Utils.log;

public class MultiMissionDownloadActivity extends AppCompatActivity {


    @BindView(R.id.toolbar)
    Toolbar toolbar;
    RxDownload rxDownload;
    String key;
    @BindView(R.id.image1)
    ImageView image1;
    @BindView(R.id.progress1)
    CircleProgressBar progress1;
    @BindView(R.id.image2)
    ImageView image2;
    @BindView(R.id.progress2)
    CircleProgressBar progress2;
    @BindView(R.id.image3)
    ImageView image3;
    @BindView(R.id.progress3)
    CircleProgressBar progress3;
    @BindView(R.id.start)
    Button start;
    @BindView(R.id.pause)
    Button pause;
    @BindView(R.id.control1)
    ImageView control1;
    @BindView(R.id.control2)
    ImageView control2;
    @BindView(R.id.control3)
    ImageView control3;


    private int state1 = DownloadFlag.NORMAL;
    private int state2 = DownloadFlag.NORMAL;
    private int state3 = DownloadFlag.NORMAL;

    private String img1 = "http://static.yingyonghui.com/icon/128/4189733.png";
    private String img2 = "http://static.yingyonghui.com/icon/128/4143651.png";
    private String img3 = "http://static.yingyonghui.com/icon/128/4256143.png";

    private String url1 = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";
    private String url2 = "http://s1.music.126.net/download/android/CloudMusic_official_3.7.3_153912.apk";
    private String url3 = "http://dldir1.qq.com/weixin/android/weixin6330android920.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_mission_download);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        rxDownload = RxDownload.getInstance(this);
        key = UUID.randomUUID().toString();

        Picasso.with(this).load(img1).into(image1);
        Picasso.with(this).load(img2).into(image2);
        Picasso.with(this).load(img3).into(image3);

        rxDownload.receiveMissionsEvent(key)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        log(downloadEvent.getFlag() + "");
                        if (downloadEvent.getFlag() == DownloadFlag.FAILED) {
                            Throwable throwable = downloadEvent.getError();
                            log(throwable);
                        }
                    }
                });
        rxDownload.receiveDownloadStatus(url1)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        int flag = downloadEvent.getFlag();
                        state1 = flag;
                        if (flag == DownloadFlag.FAILED) {
                            Throwable throwable = downloadEvent.getError();
                            log(throwable);
                        }
                        if (flag == DownloadFlag.STARTED) {
                            control1.setImageResource(R.drawable.ic_pause);
                        }
                        if (flag == DownloadFlag.PAUSED) {
                            control1.setImageResource(R.drawable.ic_play_arrow);
                        }
                        DownloadStatus status = downloadEvent.getDownloadStatus();
                        progress1.setProgress(status.getPercentNumber());
                    }
                });

        rxDownload.receiveDownloadStatus(url2)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        int flag = downloadEvent.getFlag();
                        state2 = flag;
                        if (flag == DownloadFlag.FAILED) {
                            Throwable throwable = downloadEvent.getError();
                            log(throwable);
                        }
                        if (flag == DownloadFlag.STARTED) {
                            control2.setImageResource(R.drawable.ic_pause);
                        }
                        if (flag == DownloadFlag.PAUSED) {
                            control2.setImageResource(R.drawable.ic_play_arrow);
                        }
                        DownloadStatus status = downloadEvent.getDownloadStatus();
                        progress2.setProgress(status.getPercentNumber());
                    }
                });

        rxDownload.receiveDownloadStatus(url3)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        int flag = downloadEvent.getFlag();
                        state3 = flag;
                        if (flag == DownloadFlag.FAILED) {
                            Throwable throwable = downloadEvent.getError();
                            log(throwable);
                        }
                        if (flag == DownloadFlag.STARTED) {
                            control3.setImageResource(R.drawable.ic_pause);
                        }
                        if (flag == DownloadFlag.PAUSED) {
                            control3.setImageResource(R.drawable.ic_play_arrow);
                        }
                        DownloadStatus status = downloadEvent.getDownloadStatus();
                        progress3.setProgress(status.getPercentNumber());
                    }
                });
    }

    @OnClick({R.id.start, R.id.pause})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                //批量下载
                rxDownload.serviceDownload(key, url1, url2, url3)
                        .subscribe(new Consumer<Object>() {
                            @Override
                            public void accept(Object o) throws Exception {
                                Toast.makeText(MultiMissionDownloadActivity.this, "开始", Toast.LENGTH_SHORT).show();
                            }
                        });

                break;
            case R.id.pause:
                rxDownload.pauseServiceDownload(key)
                        .subscribe();
                break;
        }
    }

    @OnClick({R.id.control1, R.id.control2, R.id.control3})
    public void onControlClick(View view) {
        switch (view.getId()) {
            case R.id.control1:
                if (state1 == DownloadFlag.NORMAL || state1 == DownloadFlag.PAUSED) {
                    rxDownload.serviceDownload(url1).subscribe();
                } else {
                    rxDownload.pauseServiceDownload(url1).subscribe();
                }
                break;
            case R.id.control2:
                if (state2 == DownloadFlag.NORMAL || state2 == DownloadFlag.PAUSED) {
                    rxDownload.serviceDownload(url2).subscribe();
                } else {
                    rxDownload.pauseServiceDownload(url2).subscribe();
                }
                break;
            case R.id.control3:
                if (state3 == DownloadFlag.NORMAL || state3 == DownloadFlag.PAUSED) {
                    rxDownload.serviceDownload(url3).subscribe();
                } else {
                    rxDownload.pauseServiceDownload(url3).subscribe();
                }
                break;
        }
    }
}
