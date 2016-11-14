package zlc.season.rxdownloadproject;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import zlc.season.rxdownloadproject.basic_download.BasicDownloadActivity;
import zlc.season.rxdownloadproject.download_manager.AppMarketActivity;
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

    private String weixin = "http://dldir1.qq.com/weixin/android/weixin6327android880.apk";

    @OnClick({R.id.basic_download, R.id.service_download, R.id.app_market})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.basic_download:
                startActivity(new Intent(this, BasicDownloadActivity.class));
                break;
            case R.id.service_download:
                startActivity(new Intent(this, ServiceDownloadActivity.class));
                break;
            case R.id.app_market:
                startActivity(new Intent(this, AppMarketActivity.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_check_update) {
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("更新")
                    .setMessage("有新版本发布")
                    .setPositiveButton("升级", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setClass(MainActivity.this, UpdateService.class);
                            intent.putExtra(UpdateService.INTENT_DOWNLOAD_URL, weixin);
                            intent.putExtra(UpdateService.INTENT_SAVE_NAME, "weixin.apk");
                            startService(intent);
                        }
                    }).create();
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

    }

}
