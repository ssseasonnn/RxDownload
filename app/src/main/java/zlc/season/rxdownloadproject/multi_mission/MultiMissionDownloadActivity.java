package zlc.season.rxdownloadproject.multi_mission;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import online.osslab.CircleProgressBar;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownloadproject.R;

import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.log;

public class MultiMissionDownloadActivity extends AppCompatActivity {


    @BindView(R.id.toolbar)
    Toolbar toolbar;

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
    Button control1;
    @BindView(R.id.control2)
    Button control2;
    @BindView(R.id.control3)
    Button control3;


    private static final String missionId = "testMissionId";

    private RxDownload rxDownload;

    private String url1 = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";
    private String url2 = "http://s1.music.126.net/download/android/CloudMusic_official_3.7.3_153912.apk";
    private String url3 = "http://dl.coolapkmarket.com/down/apk_file/2017/0301/com.ss.android.article.news-6.0.2-602-0301.apk";

    private Disposable disposable1, disposable2, disposable3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_mission_download);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        rxDownload = RxDownload.getInstance(this);

        String img1 = "http://static.yingyonghui.com/icon/128/4189733.png";
        Picasso.with(this).load(img1).into(image1);
        String img2 = "http://static.yingyonghui.com/icon/128/4143651.png";
        Picasso.with(this).load(img2).into(image2);
        String img3 = "http://static.yingyonghui.com/icon/128/4256143.png";
        Picasso.with(this).load(img3).into(image3);
    }

    @Override
    protected void onResume() {
        super.onResume();

        rxDownload.serviceMultiDownload(missionId, url1, url2, url3)
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        Toast.makeText(MultiMissionDownloadActivity.this, "开始", Toast.LENGTH_SHORT).show();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        log(throwable);
                    }
                });

        disposable1 = rxDownload.receiveDownloadStatus(url1)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        int flag = downloadEvent.getFlag();
                        switch (flag) {
                            case DownloadFlag.NORMAL:
                                control1.setVisibility(View.GONE);
                                break;
                            case DownloadFlag.WAITING:
                                control1.setVisibility(View.VISIBLE);
                                control1.setText("等待中");
                                break;
                            case DownloadFlag.STARTED:
                                control1.setText("下载中");
                                break;
                            case DownloadFlag.PAUSED:
                                control1.setText("已暂停");
                                break;
                            case DownloadFlag.COMPLETED:
                                control1.setText("已完成");
                                break;
                            case DownloadFlag.FAILED:
                                Throwable throwable = downloadEvent.getError();
                                log(throwable);
                                control1.setText("失败");
                                break;
                        }
                        DownloadStatus status = downloadEvent.getDownloadStatus();
                        progress1.setProgress(status.getPercentNumber());
                    }
                });

        disposable2 = rxDownload.receiveDownloadStatus(url2)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        int flag = downloadEvent.getFlag();
                        switch (flag) {
                            case DownloadFlag.NORMAL:
                                control2.setVisibility(View.GONE);
                                break;
                            case DownloadFlag.WAITING:
                                control2.setVisibility(View.VISIBLE);
                                control2.setText("等待中");
                                break;
                            case DownloadFlag.STARTED:
                                control2.setText("下载中");
                                break;
                            case DownloadFlag.PAUSED:
                                control2.setText("已暂停");
                                break;
                            case DownloadFlag.COMPLETED:
                                control2.setText("已完成");
                                break;
                            case DownloadFlag.FAILED:
                                Throwable throwable = downloadEvent.getError();
                                log(throwable);
                                control2.setText("失败");
                                break;
                        }
                        DownloadStatus status = downloadEvent.getDownloadStatus();
                        progress2.setProgress(status.getPercentNumber());
                    }
                });

        disposable3 = rxDownload.receiveDownloadStatus(url3)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        int flag = downloadEvent.getFlag();
                        switch (flag) {
                            case DownloadFlag.NORMAL:
                                control3.setVisibility(View.GONE);
                                break;
                            case DownloadFlag.WAITING:
                                control3.setVisibility(View.VISIBLE);
                                control3.setText("等待中");
                                break;
                            case DownloadFlag.STARTED:
                                control3.setText("下载中");
                                break;
                            case DownloadFlag.PAUSED:
                                control3.setText("已暂停");
                                break;
                            case DownloadFlag.COMPLETED:
                                control3.setText("已完成");
                                break;
                            case DownloadFlag.FAILED:
                                Throwable throwable = downloadEvent.getError();
                                log(throwable);
                                control3.setText("失败");
                                break;
                        }
                        DownloadStatus status = downloadEvent.getDownloadStatus();
                        progress3.setProgress(status.getPercentNumber());
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        dispose(disposable1);
        dispose(disposable2);
        dispose(disposable3);
    }

    @OnClick({R.id.start, R.id.pause, R.id.delete})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                rxDownload.startAll(missionId)
                        .subscribe(new Consumer<Object>() {
                            @Override
                            public void accept(Object o) throws Exception {
                                Toast.makeText(MultiMissionDownloadActivity.this, "全部开始", Toast
                                        .LENGTH_SHORT).show();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                log(throwable);
                            }
                        });

                break;
            case R.id.pause:
                rxDownload.pauseAll(missionId)
                        .subscribe(new Consumer<Object>() {
                            @Override
                            public void accept(Object o) throws Exception {
                                Toast.makeText(MultiMissionDownloadActivity.this, "全部暂停", Toast
                                        .LENGTH_SHORT).show();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                log(throwable);
                            }
                        });
                break;
            case R.id.delete:
                rxDownload.deleteAll(missionId, true)
                        .subscribe(new Consumer<Object>() {
                            @Override
                            public void accept(Object o) throws Exception {
                                Toast.makeText(MultiMissionDownloadActivity.this, "删除成功", Toast
                                        .LENGTH_SHORT).show();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                log(throwable);
                            }
                        });
        }
    }
}
