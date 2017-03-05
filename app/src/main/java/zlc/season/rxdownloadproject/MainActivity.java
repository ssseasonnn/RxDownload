package zlc.season.rxdownloadproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.function.Utils;
import zlc.season.rxdownloadproject.basic_download.BasicDownloadActivity;
import zlc.season.rxdownloadproject.download_manager.AppMarketActivity;
import zlc.season.rxdownloadproject.multi_mission.MultiMissionDownloadActivity;
import zlc.season.rxdownloadproject.service_download.ServiceDownloadActivity;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.service_download)
    Button mServiceDownload;
    @BindView(R.id.content_menu)
    LinearLayout mContentMenu;
    @BindView(R.id.fab)
    FloatingActionButton mFab;
    @BindView(R.id.basic_download)
    Button mBasicDownload;

    @OnClick({R.id.basic_download, R.id.service_download, R.id.multi_mission, R.id.app_market})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.basic_download:
                startActivity(new Intent(this, BasicDownloadActivity.class));
                break;
            case R.id.service_download:
                startActivity(new Intent(this, ServiceDownloadActivity.class));
                break;
            case R.id.multi_mission:
                startActivity(new Intent(this, MultiMissionDownloadActivity.class));
                break;
            case R.id.app_market:
                startActivity(new Intent(this, AppMarketActivity.class));
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        Utils.setDebug(true);
        RxDownload.getInstance(this)
                .maxDownloadNumber(2)
                .maxThread(3);
    }

}
