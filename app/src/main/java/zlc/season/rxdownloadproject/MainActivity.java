package zlc.season.rxdownloadproject;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import zlc.season.practicalrecyclerview.PracticalRecyclerView;
import zlc.season.rxdownload.UpdateService;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.recycler)
    PracticalRecyclerView mRecycler;
    @BindView(R.id.content_main)
    RelativeLayout mContentMain;
    @BindView(R.id.fab)
    FloatingActionButton mFab;

    private DownloadAdapter mAdapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("更新")
                .setMessage("有新版本发布")
                .setPositiveButton("升级", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, UpdateService.class);
                        intent.putExtra(UpdateService.INTENT_DOWNLOAD_URL, "http://dldir1.qq" +
                                ".com/weixin/android/weixin6327android880.apk");
                        intent.putExtra(UpdateService.INTENT_SAVE_NAME, "weixin.apk");
                        startService(intent);
                    }
                }).create();
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mAdapter = new DownloadAdapter();
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapterWithLoading(mAdapter);

        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribe();
    }

    private void loadData() {
        Resources res = getResources();
        final String[] images = res.getStringArray(R.array.image);
        final String[] urls = res.getStringArray(R.array.url);
        List<DownloadBean> list = new ArrayList<>();
        for (int i = 0; i < images.length; i++) {
            DownloadBean temp = new DownloadBean();
            temp.image = images[i];
            temp.url = urls[i];
            temp.state = DownloadBean.START;
            list.add(temp);
        }
        mAdapter.addAll(list);
    }

    private void unsubscribe() {
        List<DownloadBean> list = mAdapter.getData();
        for (DownloadBean each : list) {
            each.unsubscrbe();
        }
    }
}
