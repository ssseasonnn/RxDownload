package zlc.season.rxdownloadproject;

import android.Manifest;
import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import zlc.season.practicalrecyclerview.AbstractViewHolder;
import zlc.season.rxdownload.DownloadReceiver;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;

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
    Button mStatus;

    DownloadBean data;
    private Context mContext;

    public DownloadViewHolder(ViewGroup parent) {
        super(parent, R.layout.download_item);
        ButterKnife.bind(this, itemView);
        mContext = parent.getContext();
    }

    @Override
    public void setData(DownloadBean param) {
        this.data = param;
        Picasso.with(mContext).load(param.image).into(mImg);
        mStatus.setText("开始");

        data.mReceiver = new DownloadReceiver(new DownloadReceiver.CallBack() {
            @Override
            public void onDownloadStart() {
                data.state = DownloadBean.PAUSE;
                mStatus.setText("暂停");
            }

            @Override
            public void onDownloadNext() {
                //                mProgress.setIndeterminate(status.isChunked);
                //                mProgress.setMax((int) status.getTotalSize());
                //                mProgress.setProgress((int) status.getDownloadSize());
                //                mPercent.setText(status.getPercent());
                //                mSize.setText(status.getFormatStatusString());
            }

            @Override
            public void onDownloadComplete() {
                data.state = DownloadBean.DONE;
                mStatus.setText("已完成");
            }

            @Override
            public void onDownloadError() {
                data.state = DownloadBean.START;
                mStatus.setText("继续");
            }
        });

        mContext.registerReceiver(data.mReceiver, data.mReceiver.getFilter());
    }


    //    @OnClick(R.id.status)
    public void onClick() {
        if (data.state == DownloadBean.START) {
            data.state = DownloadBean.PAUSE;
            mStatus.setText("暂停");

            data.subscription = RxPermissions.getInstance(mContext)
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
                    .compose(RxDownload.getInstance().transform(data.url, data.name, null))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<DownloadStatus>() {
                        @Override
                        public void onCompleted() {
                            data.state = DownloadBean.DONE;
                            mStatus.setText("已完成");
                            data.unsubscrbe();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.w("TAG", e);
                            data.state = DownloadBean.START;
                            mStatus.setText("继续");
                            data.unsubscrbe();
                        }

                        @Override
                        public void onNext(final DownloadStatus status) {
                            mProgress.setIndeterminate(status.isChunked);
                            mProgress.setMax((int) status.getTotalSize());
                            mProgress.setProgress((int) status.getDownloadSize());
                            mPercent.setText(status.getPercent());
                            mSize.setText(status.getFormatStatusString());
                        }
                    });
        } else if (data.state == DownloadBean.PAUSE) {
            data.unsubscrbe();
            data.state = DownloadBean.START;
            mStatus.setText("继续");
        }
    }

    @OnClick(status)
    public void onClick1() {

        RxDownload.getInstance().serviceDownload(mContext, data.url, data.name, null);

    }
}
