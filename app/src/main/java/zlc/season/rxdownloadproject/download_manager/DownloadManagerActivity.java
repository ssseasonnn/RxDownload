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
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import zlc.season.practicalrecyclerview.PracticalRecyclerView;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadRecord;
import zlc.season.rxdownload2.function.Utils;
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
        List<DownloadItem> list = mAdapter.getData();
        for (DownloadItem each : list) {
            Utils.dispose(each.disposable);
        }
    }

    private void loadData() {
        RxDownload.getInstance(this).getTotalDownloadRecords()
                .map(new Function<List<DownloadRecord>, List<DownloadItem>>() {
                    @Override
                    public List<DownloadItem> apply(List<DownloadRecord> downloadRecords) throws Exception {
                        List<DownloadItem> result = new ArrayList<>();
                        for (DownloadRecord each : downloadRecords) {
                            DownloadItem bean = new DownloadItem();
                            bean.record = each;
                            result.add(bean);
                        }
                        return result;
                    }
                })
                .subscribe(new Consumer<List<DownloadItem>>() {
                    @Override
                    public void accept(List<DownloadItem> downloadBeen) throws Exception {
                        mAdapter.addAll(downloadBeen);
                    }
                });
    }
}
