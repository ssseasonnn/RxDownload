package zlc.season.rxdownloadproject.download_manager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
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

    private DownloadAdapter mAdapter;
    private RxDownload rxDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        rxDownload = RxDownload.getInstance(this);

        mAdapter = new DownloadAdapter();
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapterWithLoading(mAdapter);
        loadData();
    }

    @OnClick({R.id.start, R.id.pause})
    public void onClick(View view) {
        List<DownloadItem> list = mAdapter.getData();
        switch (view.getId()) {
            case R.id.start:
                for (DownloadItem each : list) {
                    rxDownload.serviceDownload(each.record.getUrl())
                            .subscribe(new Consumer<Object>() {
                                @Override
                                public void accept(Object o) throws Exception {

                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Utils.log(throwable);
                                }
                            });
                }
                break;
            case R.id.pause:
                for (DownloadItem each : list) {
                    rxDownload.pauseServiceDownload(each.record.getUrl())
                            .subscribe(new Consumer<Object>() {
                                @Override
                                public void accept(Object o) throws Exception {

                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Utils.log(throwable);
                                }
                            });
                }
                break;
        }
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
