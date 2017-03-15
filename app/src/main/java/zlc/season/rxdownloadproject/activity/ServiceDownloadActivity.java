package zlc.season.rxdownloadproject.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import io.reactivex.functions.Consumer;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownloadproject.Constants;
import zlc.season.rxdownloadproject.R;
import zlc.season.rxdownloadproject.databinding.ActivityServiceDownloadBinding;
import zlc.season.rxdownloadproject.model.DownloadController;
import zlc.season.rxdownloadproject.model.ServiceModel;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.widget.Toast.LENGTH_SHORT;
import static zlc.season.rxdownload2.function.Utils.log;

public class ServiceDownloadActivity extends AppCompatActivity {
	private String url = Constants.URL4;
	private RxDownload mRxDownload;
	private DownloadController mDownloadController;
	private ActivityServiceDownloadBinding binding;
	private ServiceModel serviceModel = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// initData
		serviceModel = new ServiceModel();
		// initView
		binding = DataBindingUtil.setContentView(this, R.layout.activity_service_download);
		binding.setItem(serviceModel);
		binding.contentServiceDownload.setPresenter(new Presenter());
		setSupportActionBar(binding.toolbar);
		mRxDownload = RxDownload.getInstance(this);
		mDownloadController = new DownloadController(binding.contentServiceDownload.status, binding.contentServiceDownload.action);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mRxDownload.receiveDownloadStatus(url)
				.subscribe(new Consumer<DownloadEvent>() {
					@Override
					public void accept(DownloadEvent downloadEvent) throws Exception {
						if (downloadEvent.getFlag() == DownloadFlag.FAILED) {
							Throwable throwable = downloadEvent.getError();
							log(throwable);
						}
						mDownloadController.setEvent(downloadEvent);
						updateProgress(downloadEvent);
					}
				});
	}

	private void updateProgress(DownloadEvent event) {
		DownloadStatus status = event.getDownloadStatus();
		binding.contentServiceDownload.progress.setIndeterminate(status.isChunked);
		binding.contentServiceDownload.progress.setMax((int) status.getTotalSize());
		binding.contentServiceDownload.progress.setProgress((int) status.getDownloadSize());
		serviceModel.setPercent(status.getPercent());
		serviceModel.setSize(status.getFormatStatusString());
	}

	private void start() {
		RxPermissions.getInstance(this)
				.request(WRITE_EXTERNAL_STORAGE)
				.doOnNext(new Consumer<Boolean>() {
					@Override
					public void accept(Boolean granted) throws Exception {
						if (!granted) {
							throw new RuntimeException("no permission");
						}
					}
				})
				.compose(mRxDownload.<Boolean>transformService(url))
				.subscribe(new Consumer<Object>() {
					@Override
					public void accept(Object o) throws Exception {
						Toast.makeText(ServiceDownloadActivity.this, "下载开始", LENGTH_SHORT).show();
					}
				});
	}

	private void pause() {
		mRxDownload.pauseServiceDownload(url).subscribe();
	}

	private void installApk() {
		File[] files = mRxDownload.getRealFiles(url);
		if (files != null) {
			Uri uri = Uri.fromFile(files[0]);
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.setDataAndType(uri, "application/vnd.android.package-archive");
			startActivity(intent);
		} else {
			Toast.makeText(this, "File not exists", Toast.LENGTH_SHORT).show();
		}
	}

	public class Presenter {
		public void onClick(View view) {
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
				public void install() {
					installApk();
				}
			});
		}

		public void onClickFinish(View view) {
			ServiceDownloadActivity.this.finish();
		}
	}
}
