package zlc.season.rxdownloadproject.download_manager;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import zlc.season.practicalrecyclerview.PracticalRecyclerView;
import zlc.season.practicalrecyclerview.SectionItem;
import zlc.season.rxdownloadproject.R;

public class DownloadManagerActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.recycler)
    PracticalRecyclerView mRecycler;
    @BindView(R.id.content_main)
    RelativeLayout mContentMain;

    private DownloadAdapter mAdapter;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mAdapter = new DownloadAdapter();
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapterWithLoading(mAdapter);

        mAdapter.addFooter(new Footer());
        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribe();
    }

    private void loadData() {
        Resources res = getResources();
        final String[] names = res.getStringArray(R.array.save_name);
        final String[] images = res.getStringArray(R.array.image);
        final String[] urls = res.getStringArray(R.array.url);
        List<DownloadBean> list = new ArrayList<>();
        for (int i = 0; i < images.length; i++) {
            DownloadBean temp = new DownloadBean();
            temp.name = names[i];
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

    class Footer implements SectionItem {

        @BindView(R.id.finish)
        Button mFinish;

        @Override
        public View createView(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.foot_layout, parent, false);
            ButterKnife.bind(this, view);
            return view;
        }

        @Override
        public void onBind() {

        }

        @OnClick(R.id.finish)
        public void onClick() {
            DownloadManagerActivity.this.finish();
        }
    }
}
