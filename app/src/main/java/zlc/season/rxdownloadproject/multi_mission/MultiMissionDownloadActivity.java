package zlc.season.rxdownloadproject.multi_mission;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadBean;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownloadproject.R;

import static zlc.season.rxdownload2.function.Utils.log;

public class MultiMissionDownloadActivity extends AppCompatActivity {

    @BindView(R.id.image1)
    ImageView image1;
    @BindView(R.id.image2)
    ImageView image2;
    @BindView(R.id.image3)
    ImageView image3;
    @BindView(R.id.content)
    LinearLayout content;
    @BindView(R.id.progress)
    LinearLayout progress;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    RxDownload rxDownload;
    String key;
    private String url1 = "http://static.yingyonghui.com/icon/128/4189733.png";
    private String url2 = "http://static.yingyonghui.com/icon/128/4143651.png";
    private String url3 = "http://static.yingyonghui.com/icon/128/4256143.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_mission_download);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        rxDownload = RxDownload.getInstance(this);
        key = UUID.randomUUID().toString();
    }

    @OnClick({R.id.start, R.id.pause})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                List<DownloadBean> list = new ArrayList<>();
                DownloadBean bean1 = new DownloadBean.Builder(url1).build();
                DownloadBean bean2 = new DownloadBean.Builder(url2).build();
                DownloadBean bean3 = new DownloadBean.Builder(url3).build();
                list.add(bean1);
                list.add(bean2);
                list.add(bean3);
                rxDownload.serviceDownload(list, key)
                        .subscribe(new Consumer<Object>() {
                            @Override
                            public void accept(Object o) throws Exception {
                                Toast.makeText(MultiMissionDownloadActivity.this, "开始", Toast.LENGTH_SHORT).show();
                            }
                        });
                rxDownload.receiveDownloadStatus(key)
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
                break;
            case R.id.pause:
                break;
        }
    }
}
