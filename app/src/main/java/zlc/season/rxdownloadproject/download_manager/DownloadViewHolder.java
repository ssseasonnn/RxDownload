package zlc.season.rxdownloadproject.download_manager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import zlc.season.practicalrecyclerview.AbstractAdapter;
import zlc.season.practicalrecyclerview.AbstractViewHolder;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownload2.function.Utils;
import zlc.season.rxdownloadproject.DownloadController;
import zlc.season.rxdownloadproject.R;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.empty;
import static zlc.season.rxdownload2.function.Utils.log;
import static zlc.season.rxdownloadproject.R.id.percent;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/28
 * Time: 09:37
 * FIXME
 */
public class DownloadViewHolder extends AbstractViewHolder<DownloadItem> {
    @BindView(R.id.img)
    ImageView mImg;
    @BindView(percent)
    TextView mPercent;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.size)
    TextView mSize;
    @BindView(R.id.status)
    TextView mStatusText;
    @BindView(R.id.action)
    Button mActionButton;
    @BindView(R.id.name)
    TextView mName;
    @BindView(R.id.more)
    Button mMore;

    private AbstractAdapter mAdapter;
    private DownloadController mDownloadController;

    private Context mContext;
    private DownloadItem data;

    private RxDownload mRxDownload;
    private int flag;

    public DownloadViewHolder(ViewGroup parent, AbstractAdapter adapter) {
        super(parent, R.layout.download_manager_item);
        ButterKnife.bind(this, itemView);
        this.mAdapter = adapter;
        mContext = parent.getContext();

        mRxDownload = RxDownload.getInstance(mContext);

        mDownloadController = new DownloadController(mStatusText, mActionButton);
    }

    @Override
    public void setData(DownloadItem param) {
        this.data = param;
        if (empty(param.record.getExtra1())) {
            Picasso.with(mContext).load(R.mipmap.ic_file_download).into(mImg);
        } else {
            Picasso.with(mContext).load(param.record.getExtra1()).into(mImg);
        }

        String name = empty(param.record.getExtra2()) ? param.record.getSaveName() : param.record.getExtra2();
        mName.setText(name);


        Utils.log(data.record.getUrl());
        data.disposable = mRxDownload.receiveDownloadStatus(data.record.getUrl())
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        if (flag != downloadEvent.getFlag()) {
                            flag = downloadEvent.getFlag();
                            log(flag + "");
                        }

                        if (downloadEvent.getFlag() == DownloadFlag.FAILED) {
                            Throwable throwable = downloadEvent.getError();
                            Log.w("TAG", throwable);
                        }
                        mDownloadController.setEvent(downloadEvent);
                        updateProgressStatus(downloadEvent.getDownloadStatus());
                    }
                });
    }

    @OnClick({R.id.action, R.id.more})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action:
                mDownloadController.handleClick(new DownloadController.Callback() {
                    @Override
                    public void startDownload() {
                        start();
                    }

                    @Override
                    public void pauseDownload() {
                        pause();
                    }

                    @Override
                    public void install() {
                        installApk();
                    }
                });
                break;
            case R.id.more:
                showPopUpWindow(view);
                break;
        }
    }

    private void updateProgressStatus(DownloadStatus status) {
        mProgress.setIndeterminate(status.isChunked);
        mProgress.setMax((int) status.getTotalSize());
        mProgress.setProgress((int) status.getDownloadSize());
        mPercent.setText(status.getPercent());
        mSize.setText(status.getFormatStatusString());
    }

    private void installApk() {
        File[] files = mRxDownload.getRealFiles(data.record.getUrl());
        if (files != null) {
            Uri uri = Uri.fromFile(files[0]);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            mContext.startActivity(intent);
        } else {
            Toast.makeText(mContext, "File not exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void start() {
        RxPermissions.getInstance(mContext)
                .request(WRITE_EXTERNAL_STORAGE)
                .doOnNext(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) throws Exception {
                        if (!granted) {
                            throw new RuntimeException("no permission");
                        }
                    }
                })
                .compose(mRxDownload.<Boolean>transformService(data.record.getUrl()))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        Toast.makeText(mContext, "下载开始", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void pause() {
        mRxDownload.pauseServiceDownload(data.record.getUrl()).subscribe();
    }

    private void delete() {
        dispose(data.disposable);
        mRxDownload.deleteServiceDownload(data.record.getUrl(), true)
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        mAdapter.remove(getAdapterPosition());
                    }
                })
                .subscribe();

    }

    private void showPopUpWindow(View view) {
        final ListPopupWindow listPopupWindow = new ListPopupWindow(mContext);
        listPopupWindow.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1,
                new String[]{"删除"}));
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    delete();
                    listPopupWindow.dismiss();
                }
            }
        });
        listPopupWindow.setWidth(200);
        listPopupWindow.setAnchorView(view);
        listPopupWindow.setModal(false);
        listPopupWindow.show();
    }
}
