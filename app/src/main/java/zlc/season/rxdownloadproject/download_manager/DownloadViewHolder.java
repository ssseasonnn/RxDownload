package zlc.season.rxdownloadproject.download_manager;

import android.Manifest;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.Disposable;
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

import static zlc.season.rxdownloadproject.R.id.percent;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/28
 * Time: 09:37
 * FIXME
 */
public class DownloadViewHolder extends AbstractViewHolder<DownloadBean> {
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
    private Context mContext;
    private DownloadBean mData;
    private RxDownload mRxDownload;
    private DownloadController mDownloadController;
    private Disposable mDisposable;

    public DownloadViewHolder(ViewGroup parent, AbstractAdapter adapter) {
        super(parent, R.layout.download_manager_item);
        ButterKnife.bind(this, itemView);
        this.mAdapter = adapter;
        mContext = parent.getContext();
        mRxDownload = RxDownload.getInstance().context(mContext);

        mDownloadController = new DownloadController(mStatusText, mActionButton);
    }

    @Override
    public void setData(DownloadBean param) {
        this.mData = param;
        Picasso.with(mContext).load(R.mipmap.ic_file_download).into(mImg);
        mName.setText(param.mRecord.getSaveName());

        /**
         * important!! 如果有订阅没有取消,则取消订阅!防止ViewHolder复用导致界面显示的BUG!
         */
        Utils.dispose(mDisposable);
        mDisposable = mRxDownload.receiveDownloadStatus(mData.mRecord.getUrl())
                                 .subscribe(new Consumer<DownloadEvent>() {
                                     @Override
                                     public void accept(DownloadEvent downloadEvent)
                                             throws Exception {
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
                    public void cancelDownload() {
                        cancel();
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
        Uri uri = Uri.fromFile(mRxDownload
                .getRealFiles(mData.mRecord.getSaveName(), mData.mRecord.getSavePath())[0]);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }

    private void start() {
        String url = mData.mRecord.getUrl();
        String saveName = mData.mRecord.getSaveName();
        String savePath = mData.mRecord.getSavePath();

        RxPermissions.getInstance(mContext)
                     .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                     .doOnNext(new Consumer<Boolean>() {
                         @Override
                         public void accept(Boolean granted) throws Exception {
                             if (!granted) {
                                 throw new RuntimeException("no permission");
                             }
                         }
                     })
                     .compose(mRxDownload.<Boolean>transformService(url, saveName, savePath))
                     .subscribe(new Consumer<Object>() {
                         @Override
                         public void accept(Object o) throws Exception {
                             Toast.makeText(mContext, "下载开始", Toast.LENGTH_SHORT).show();
                         }
                     });
    }

    private void pause() {
        mRxDownload.pauseServiceDownload(mData.mRecord.getUrl()).subscribe();
    }

    private void cancel() {
        mRxDownload.cancelServiceDownload(mData.mRecord.getUrl()).subscribe();
    }

    private void delete() {
        mRxDownload.deleteServiceDownload(mData.mRecord.getUrl(), true)
                   .subscribe(new Consumer<Object>() {
                       @Override
                       public void accept(Object o) throws Exception {
                           Utils.dispose(mDisposable);
                           mAdapter.remove(getAdapterPosition());
                       }
                   });
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
