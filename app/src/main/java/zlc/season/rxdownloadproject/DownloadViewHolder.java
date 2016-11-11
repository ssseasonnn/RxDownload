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
        super(parent, R.layout.service_download_item);
        ButterKnife.bind(this, itemView);
        mContext = parent.getContext();
    }

    @Override
    public void setData(DownloadBean param) {
        this.data = param;
        Picasso.with(mContext).load(param.image).into(mImg);
        mStatus.setText("开始");

        data.mReceiver = new DownloadReceiver(data.url, new DownloadReceiver.CallBack() {
            @Override
            public void onDownloadStart() {
                data.state = DownloadBean.PAUSE;
                mStatus.setText("暂停");
            }

            @Override
            public void onDownloadNext(DownloadStatus status) {
                mProgress.setIndeterminate(status.isChunked);
                mProgress.setMax((int) status.getTotalSize());
                mProgress.setProgress((int) status.getDownloadSize());
                mPercent.setText(status.getPercent());
                mSize.setText(status.getFormatStatusString());
            }


            @Override
            public void onDownloadComplete() {
                data.state = DownloadBean.DONE;
                mStatus.setText("已完成");
            }

            @Override
            public void onDownloadError(Throwable e) {
                data.state = DownloadBean.START;
                mStatus.setText("继续");
            }
        });

        mContext.registerReceiver(data.mReceiver, data.mReceiver.getFilter());
    }


    //    @OnClick(R.id.status)
    public void onClick() {

    }

    @OnClick(status)
    public void onClick1() {

        RxDownload.getInstance().serviceDownload(mContext, data.url, data.name, null);

    }
}
