package zlc.season.rxdownloadproject.download_manager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import zlc.season.practicalrecyclerview.AbstractAdapter;
import zlc.season.practicalrecyclerview.AbstractViewHolder;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownload.entity.DownloadFlag;
import zlc.season.rxdownload.entity.DownloadStatus;
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
    @BindView(R.id.delete)
    Button mDelete;
    @BindView(R.id.cancel)
    Button mCancel;

    private AbstractAdapter mAdapter;
    private Context mContext;
    private DownloadBean mData;
    private RxDownload mRxDownload;

    private DownloadController mDownloadController;

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

        initFirstState(param);

        //接收下载进度
        Subscription temp = mRxDownload.receiveDownloadStatus(mData.mRecord.getUrl())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        mDownloadController.setStateAndDisplay(DownloadFlag.COMPLETED);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mDownloadController.setStateAndDisplay(DownloadFlag.FAILED);
                    }

                    @Override
                    public void onNext(final DownloadStatus status) {
                        updateProgressStatus(status);
                    }
                });

        mData.mSubscriptions.add(temp);
    }

    @OnClick({R.id.action, R.id.cancel, R.id.delete})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action:
                mDownloadController.performClick(new DownloadController.Callback() {
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
                    }

                    @Override
                    public void install() {
                        installApk();
                    }
                });
                break;
            case R.id.cancel:
                cancel();
                break;
            case R.id.delete:
                delete();
                break;
        }
    }

    private void initFirstState(DownloadBean param) {
        Picasso.with(mContext).load(R.mipmap.ic_file_download).into(mImg);
        mName.setText(param.mRecord.getSaveName());

        int flag = param.mRecord.getDownloadFlag();
        mDownloadController.setStateAndDisplay(flag);

        //如果读取出来是已取消或已完成状态, 特殊处理一下,显示删除按钮
        if (flag == DownloadFlag.CANCELED || flag == DownloadFlag.COMPLETED) {
            mCancel.setVisibility(View.GONE);
            mDelete.setVisibility(View.VISIBLE);
        }

        updateProgressStatus(param.mRecord.getStatus());
    }

    private void updateProgressStatus(DownloadStatus status) {
        mProgress.setIndeterminate(status.isChunked);
        mProgress.setMax((int) status.getTotalSize());
        mProgress.setProgress((int) status.getDownloadSize());
        mPercent.setText(status.getPercent());
        mSize.setText(status.getFormatStatusString());
    }

    private void installApk() {
        mDownloadController.setStateAndDisplay(DownloadFlag.INSTALL);
        Uri uri = Uri.fromFile(new File(mData.mRecord.getSavePath() + File.separator + mData.mRecord.getSaveName()));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }

    private void start() {
        Subscription temp = RxPermissions.getInstance(mContext)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (!granted) {
                            throw new RuntimeException("no permission");
                        }
                    }
                })
                .observeOn(Schedulers.io())
                .compose(mRxDownload.transformServiceWithoutStatus(mData.mRecord.getUrl(), mData.mRecord.getSaveName(),
                        mData.mRecord.getSavePath()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mDownloadController.setStateAndDisplay(DownloadFlag.STARTED);
                        mDelete.setVisibility(View.GONE);
                        mCancel.setVisibility(View.VISIBLE);
                    }
                });
        mData.mSubscriptions.add(temp);
    }

    private void pause() {
        Subscription subscription = mRxDownload.pauseServiceDownload(mData.mRecord.getUrl())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mDownloadController.setStateAndDisplay(DownloadFlag.PAUSED);
                    }
                });
        mData.mSubscriptions.add(subscription);
    }

    private void cancel() {
        Subscription subscription = mRxDownload.cancelServiceDownload(mData.mRecord.getUrl())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mDownloadController.setStateAndDisplay(DownloadFlag.CANCELED);
                        mCancel.setVisibility(View.GONE);
                        mDelete.setVisibility(View.VISIBLE);
                    }
                });
        mData.mSubscriptions.add(subscription);
    }

    private void delete() {
        Subscription subscription = mRxDownload.deleteServiceDownload(mData.mRecord.getUrl())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mData.mSubscriptions.clear();
                        mAdapter.remove(getAdapterPosition());
                    }
                });
        mData.mSubscriptions.add(subscription);
    }
}
