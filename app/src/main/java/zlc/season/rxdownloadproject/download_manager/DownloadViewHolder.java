package zlc.season.rxdownloadproject.download_manager;

import android.content.Context;
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
import zlc.season.practicalrecyclerview.AbstractViewHolder;
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
    Button mStatus;
    @BindView(R.id.delete)
    Button mDelete;

    private DownloadBean data;
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
    }


    @OnClick({R.id.status, R.id.delete})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.status:
                break;
            case R.id.delete:
                break;
        }
    }
}
