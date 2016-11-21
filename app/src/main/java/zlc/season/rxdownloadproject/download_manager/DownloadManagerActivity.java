package zlc.season.rxdownloadproject.download_manager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import zlc.season.practicalrecyclerview.PracticalRecyclerView;
import zlc.season.rxdownload.entity.DownloadRecord;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownloadproject.R;

public class DownloadManagerActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.recycler)
    PracticalRecyclerView mRecycler;
    @BindView(R.id.content_main)
    RelativeLayout mContentMain;

    private DownloadAdapter mAdapter;
    private Subscription mSubscription;

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
        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribe();
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
    }

    private void loadData() {
        mSubscription = RxDownload.getInstance().context(this).getTotalDownloadRecords()
                .map(new Func1<List<DownloadRecord>, List<DownloadBean>>() {
                    @Override
                    public List<DownloadBean> call(List<DownloadRecord> downloadRecords) {
                        List<DownloadBean> result = new ArrayList<>();
                        for (DownloadRecord each : downloadRecords) {
                            DownloadBean bean = new DownloadBean();
                            bean.mRecord = each;
                            result.add(bean);
                        }
                        return result;
                    }
                })
                .subscribe(new Action1<List<DownloadBean>>() {
                    @Override
                    public void call(List<DownloadBean> downloadBeen) {
                        mAdapter.addAll(downloadBeen);
                    }
                });
    }

    private void unsubscribe() {
        List<DownloadBean> list = mAdapter.getData();
        for (DownloadBean each : list) {
            each.unsubscrbe();
        }
    }
}
