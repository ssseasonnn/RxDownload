package zlc.season.rxdownloadproject.download_manager;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

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
import zlc.season.rxdownloadproject.R;

import static zlc.season.rxdownloadproject.R.id.percent;
import static zlc.season.rxdownloadproject.R.id.status;

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
    @BindView(status)
    Button mStatusText;
    @BindView(R.id.delete)
    Button mDelete;
    @BindView(R.id.start)
    Button mStartButton;

    private Context mContext;
    private DownloadBean data;
    private RxDownload mRxDownload;

    public DownloadViewHolder(ViewGroup parent) {
        super(parent, R.layout.service_download_item);
        ButterKnife.bind(this, itemView);
        mContext = parent.getContext();
        mRxDownload = RxDownload.getInstance().context(mContext);
    }

    @Override
    public void setData(DownloadBean param) {
        this.data = param;
        Picasso.with(mContext).load(param.mRecord.getImage()).into(mImg);
        mProgress.setIndeterminate(param.mRecord.getStatus().isChunked);
        mProgress.setMax((int) param.mRecord.getStatus().getTotalSize());
        mProgress.setProgress((int) param.mRecord.getStatus().getDownloadSize());
        mPercent.setText(param.mRecord.getStatus().getPercent());
        mSize.setText(param.mRecord.getStatus().getFormatStatusString());

        int flag = param.mRecord.getDownloadFlag();
        switch (flag) {
            case DownloadRecord.FLAG_STARTED:
                mStatusText.setText("正在下载...");
                mStartButton.setText("暂停");
                break;
            case DownloadRecord.FLAG_PAUSED:
                mStatusText.setText("下载已暂停...");
                mStartButton.setText("继续");
                break;
            case DownloadRecord.FLAG_FAILED:
                mStatusText.setText("下载失败");
                mStartButton.setText("重试");
                break;
            case DownloadRecord.FLAG_CANCELED:
                mStatusText.setText("下载已取消");
                mStartButton.setText("开始");
                break;
            case DownloadRecord.FLAG_COMPLETED:
                mStatusText.setText("下载已完成");
                mStartButton.setText("");
                break;
        }
    }


    @OnClick({R.id.start, R.id.delete})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                String url = data.mRecord.getUrl();
                String saveName = data.mRecord.getSaveName();
                String savePath = data.mRecord.getSavePath();

                int flag = data.mRecord.getDownloadFlag();
                switch (flag) {
                    case DownloadRecord.FLAG_STARTED:
                        Subscription pause = mRxDownload.pauseServiceDownload(url)
                                .subscribe(new Action1<Object>() {
                                    @Override
                                    public void call(Object o) {
                                        mStatusText.setText("下载已暂停...");
                                        mStartButton.setText("继续");
                                    }
                                });
                        data.mSubscriptions.add(pause);
                        break;
                    case DownloadRecord.FLAG_PAUSED:
                    case DownloadRecord.FLAG_FAILED:
                    case DownloadRecord.FLAG_CANCELED:
                        Subscription start = startDownload(url, saveName, savePath);
                        data.mSubscriptions.add(start);
                        break;
                    case DownloadRecord.FLAG_COMPLETED:
                        //点击安装
                        break;
                }
                break;
            case R.id.delete:
                Subscription cancel = mRxDownload.cancelServiceDownload(data.mRecord.getUrl())
                        .subscribe(new Action1<Object>() {
                            @Override
                            public void call(Object o) {
                                mStatusText.setText("下载已取消");
                                mStartButton.setText("开始");
                                data.mRecord.setDownloadFlag(DownloadRecord.FLAG_CANCELED);
                            }
                        });
                data.mSubscriptions.add(cancel);
                break;
        }
    }

    private Subscription startDownload(String url, String saveName, String savePath) {
        return mRxDownload.downloadThroughService(url, saveName, savePath, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        mStatusText.setText("下载已完成");
                        mStartButton.setText("已完成");
                        data.mRecord.setDownloadFlag(DownloadRecord.FLAG_COMPLETED);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mStatusText.setText("下载失败");
                        mStartButton.setText("重试");
                        data.mRecord.setDownloadFlag(DownloadRecord.FLAG_FAILED);
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        mProgress.setIndeterminate(status.isChunked);
                        mProgress.setMax((int) status.getTotalSize());
                        mProgress.setProgress((int) status.getDownloadSize());
                        mPercent.setText(status.getPercent());
                        mSize.setText(status.getFormatStatusString());
                    }
                });
    }
}
