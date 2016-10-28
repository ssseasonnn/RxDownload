package zlc.season.rxdownloadproject;

import android.content.Context;
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
import rx.schedulers.Schedulers;
import zlc.season.practicalrecyclerview.AbstractViewHolder;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;

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
    Button mStatus;

    DownloadBean data;
    private Context mContext;
    private Subscription subscription;

    public DownloadViewHolder(ViewGroup parent) {
        super(parent, R.layout.download_item);
        ButterKnife.bind(this, itemView);
        mContext = parent.getContext();
    }

    @Override
    public void setData(DownloadBean data) {
        this.data = data;
        Picasso.with(mContext).load(data.image).into(mImg);
        mStatus.setText("开始");
    }

    @OnClick(R.id.status)
    public void onClick() {
        if (data.state == DownloadBean.START) {
            subscription = RxDownload.getInstance()
                    .download(data.url, null, null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<DownloadStatus>() {
                        @Override
                        public void onCompleted() {
                            data.state = DownloadBean.DONE;
                            mStatus.setText("已完成");
                        }

                        @Override
                        public void onError(Throwable e) {
                            data.state = DownloadBean.START;
                            mStatus.setText("继续");
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
            data.state = DownloadBean.PAUSE;
            mStatus.setText("暂停");
        } else if (data.state == DownloadBean.PAUSE) {
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
            data.state = DownloadBean.START;
            mStatus.setText("继续");
        }
    }
}
