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
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import zlc.season.practicalrecyclerview.AbstractViewHolder;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.function.Utils;
import zlc.season.rxdownloadproject.DownloadController;
import zlc.season.rxdownloadproject.R;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 09:43
 * FIXME
 */
public class AppInfoViewHolder extends AbstractViewHolder<AppInfoBean> {
    @BindView(R.id.head)
    ImageView mHead;
    @BindView(R.id.title)
    TextView mTitle;
    @BindView(R.id.content)
    TextView mContent;
    @BindView(R.id.action)
    Button mAction;

    private AppInfoBean mData;
    private Context mContext;
    private RxDownload mRxDownload;
    private DownloadController mDownloadController;
    private Disposable mDisposable;

    public AppInfoViewHolder(ViewGroup parent) {
        super(parent, R.layout.app_info_item);
        ButterKnife.bind(this, itemView);
        mContext = parent.getContext();

        mRxDownload = RxDownload.getInstance()
                .context(mContext)
                .autoInstall(true)  // 下载完成自动安装
                .maxDownloadNumber(2);//最大下载数量


        mDownloadController = new DownloadController(new TextView(mContext), mAction);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
    }

    @Override
    public void setData(AppInfoBean data) {
        this.mData = data;
        Picasso.with(mContext).load(data.img).into(mHead);
        mTitle.setText(data.name);
        mContent.setText(data.info);

        /**
         * important!! 如果有订阅没有取消,则取消订阅!防止ViewHolder复用导致界面显示的BUG!
         */
        Utils.dispose(mDisposable);
        mDisposable = mRxDownload.receiveDownloadStatus(mData.downloadUrl)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        if (downloadEvent.getFlag() == DownloadFlag.FAILED) {
                            Throwable throwable = downloadEvent.getError();
                            Log.w("TAG", throwable);
                        }
                        mDownloadController.setEvent(downloadEvent);
                    }
                });
    }

    @OnClick(R.id.action)
    public void onClick() {
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
    }

    private void installApk() {
        Uri uri = Uri.fromFile(mRxDownload.getRealFiles(mData.saveName, null)[0]);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }

    private void start() {
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
                .compose(mRxDownload.<Boolean>transformService(mData.downloadUrl, mData.saveName, null))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        Toast.makeText(mContext, "下载开始", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void pause() {
        mRxDownload.pauseServiceDownload(mData.downloadUrl).subscribe();
    }

    private void cancel() {
        mRxDownload.cancelServiceDownload(mData.downloadUrl).subscribe();
    }
}
