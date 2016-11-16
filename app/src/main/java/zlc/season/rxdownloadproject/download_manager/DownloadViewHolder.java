package zlc.season.rxdownloadproject.download_manager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
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
import zlc.season.practicalrecyclerview.AbstractViewHolder;
import zlc.season.rxdownload.DownloadRecord;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownloadproject.DownloadStateContext;
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
    @BindView(R.id.cancel)
    Button mDelete;
    @BindView(R.id.action)
    Button mActionButton;
    @BindView(R.id.name)
    TextView mName;

    private Context mContext;
    private DownloadBean mData;
    private RxDownload mRxDownload;

    private DownloadStateContext mStateContext;

    public DownloadViewHolder(ViewGroup parent) {
        super(parent, R.layout.download_manager_item);
        ButterKnife.bind(this, itemView);
        mContext = parent.getContext();
        mRxDownload = RxDownload.getInstance().context(mContext);

        mStateContext = new DownloadStateContext(mStatusText, mActionButton);
        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_NORMAL);
    }

    @Override
    public void setData(DownloadBean param) {
        this.mData = param;

        initFirstState(param);

        //注册广播接收器, 用于接收下载进度
        Subscription temp = mRxDownload.registerReceiver(mData.mRecord.getUrl())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_COMPLETED);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_FAILED);
                    }

                    @Override
                    public void onNext(final DownloadStatus status) {
                        updateProgressStatus(status);
                    }
                });

        //将subscription收集起来,在Activity销毁的时候取消订阅,以免内存泄漏
        mData.mSubscriptions.add(temp);
    }

    @OnClick({R.id.action, R.id.cancel})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action:
                mStateContext.performClick(new DownloadStateContext.Callback() {
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
            case R.id.cancel:
                cancel();
                break;
        }
    }

    //设置初始状态
    private void initFirstState(DownloadBean param) {
        if (TextUtils.isEmpty(param.mRecord.getImage())) {
            Picasso.with(mContext).load(R.mipmap.ic_file_download).into(mImg);
        } else {
            Picasso.with(mContext).load(param.mRecord.getImage()).into(mImg);
        }
        if (TextUtils.isEmpty(param.mRecord.getName())) {
            mName.setText(param.mRecord.getSaveName());
        } else {
            mName.setText(param.mRecord.getName());
        }

        int flag = param.mRecord.getDownloadFlag();
        mStateContext.setStateAndDisplay(flag);
        updateProgressStatus(param.mRecord.getStatus());
    }

    //更新下载进度
    private void updateProgressStatus(DownloadStatus status) {
        mProgress.setIndeterminate(status.isChunked);
        mProgress.setMax((int) status.getTotalSize());
        mProgress.setProgress((int) status.getDownloadSize());
        mPercent.setText(status.getPercent());
        mSize.setText(status.getFormatStatusString());
    }

    //下载完成自动打开安装程序
    private void installApk() {
        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_INSTALL);
        Uri uri = Uri.fromFile(new File(mData.mRecord.getSavePath() + File.separator + mData.mRecord.getSaveName()));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }

    //开始下载, 先检查权限
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
                .compose(mRxDownload.transformServiceNoReceiver(mData.mRecord.getUrl(), mData.mRecord.getSaveName(),
                        mData.mRecord.getSavePath()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_STARTED);
                    }
                });
        mData.mSubscriptions.add(temp);
    }

    //暂停下载
    private void pause() {
        Subscription subscription = mRxDownload.pauseServiceDownload(mData.mRecord.getUrl())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_PAUSED);
                    }
                });
        mData.mSubscriptions.add(subscription);
    }

    //取消下载
    private void cancel() {
        Subscription subscription = mRxDownload.cancelServiceDownload(mData.mRecord.getUrl())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_CANCELED);
                    }
                });
        mData.mSubscriptions.add(subscription);
    }
}
