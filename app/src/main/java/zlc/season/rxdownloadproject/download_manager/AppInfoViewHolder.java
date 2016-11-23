package zlc.season.rxdownloadproject.download_manager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import zlc.season.practicalrecyclerview.AbstractViewHolder;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownload.entity.DownloadEvent;
import zlc.season.rxdownload.function.Utils;
import zlc.season.rxdownloadproject.DownloadController;
import zlc.season.rxdownloadproject.R;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

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
    Subscription mSubscription;

    private String defaultPath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();

    private AppInfoBean mData;
    private Context mContext;
    private RxDownload mRxDownload;
    private DownloadController mDownloadController;

    public AppInfoViewHolder(ViewGroup parent) {
        super(parent, R.layout.app_info_item);
        ButterKnife.bind(this, itemView);
        mContext = parent.getContext();
        mRxDownload = RxDownload.getInstance().context(mContext);


    }

    @Override
    public void setData(AppInfoBean data) {
        this.mData = data;
        Picasso.with(mContext).load(data.img).into(mHead);
        mTitle.setText(data.name);
        mContent.setText(data.info);
        Utils.unSubscribe(mSubscription);
        mDownloadController = new DownloadController(new TextView(mContext), mAction);
        mSubscription = mRxDownload.receiveDownloadStatus(mData.downloadUrl)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadEvent>() {
                    @Override
                    public void onCompleted() {
                        mDownloadController.setState(new DownloadController.Completed());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mDownloadController.setState(new DownloadController.Failed());
                    }

                    @Override
                    public void onNext(final DownloadEvent event) {
                        mDownloadController.setEvent(event);
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
        Uri uri = Uri.fromFile(new File(defaultPath + File.separator + mData.saveName));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }

    private void start() {
        RxPermissions.getInstance(mContext)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (!granted) {
                            throw new RuntimeException("no permission");
                        }
                    }
                })
                .compose(mRxDownload.transformService(mData.downloadUrl, mData.saveName, null))
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
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
