package zlc.season.rxdownloadproject.activity;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownloadproject.Constants;
import zlc.season.rxdownloadproject.R;
import zlc.season.rxdownloadproject.databinding.ActivityBasicDownloadBinding;
import zlc.season.rxdownloadproject.model.BaseModel;
import zlc.season.rxdownloadproject.model.DownloadController;

import static zlc.season.rxdownload2.function.Utils.dispose;

public class BasicDownloadActivity extends AppCompatActivity {
	private String url = Constants.URL;
	private Disposable disposable;
	private RxDownload rxDownload;
	private DownloadController downloadController;
	private ActivityBasicDownloadBinding binding = null;
	private BaseModel baseModel = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// initData
		baseModel = new BaseModel();
		// initView
		binding = DataBindingUtil.setContentView(this, R.layout.activity_basic_download);
		binding.setItem(baseModel);
		binding.contentBasicDownload.setPresenter(new Presenter());
		setSupportActionBar(binding.toolbar);
		//
		rxDownload = RxDownload.getInstance(this);
		downloadController = new DownloadController(binding.contentBasicDownload.status, binding.contentBasicDownload.action);
		downloadController.setState(new DownloadController.Normal());
	}

	public class Presenter {
		public void onClick(View view) {
			switch (view.getId()) {
				case R.id.action:
					downloadController.handleClick(new DownloadController.Callback() {
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
				case R.id.finish:
					BasicDownloadActivity.this.finish();
					break;
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		dispose(disposable);
	}

	private void start() {
		RxPermissions.getInstance(this)
				.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
				.doOnNext(new Consumer<Boolean>() {
					@Override
					public void accept(Boolean aBoolean) throws Exception {
						if (!aBoolean) {
							throw new RuntimeException("no permission");
						}
					}
				})
				.observeOn(Schedulers.io())
				.compose(rxDownload.<Boolean>transform(url))
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<DownloadStatus>() {
					@Override
					public void onSubscribe(Disposable d) {
						disposable = d;
						downloadController.setState(new DownloadController.Started());
					}

					@Override
					public void onNext(DownloadStatus status) {
						binding.contentBasicDownload.progress.setIndeterminate(status.isChunked);
						binding.contentBasicDownload.progress.setMax((int) status.getTotalSize());
						binding.contentBasicDownload.progress.setProgress((int) status.getDownloadSize());
						baseModel.setPercent(status.getPercent());
						baseModel.setSize(status.getFormatStatusString());
					}

					@Override
					public void onError(Throwable e) {
						downloadController.setState(new DownloadController.Paused());
					}

					@Override
					public void onComplete() {
						downloadController.setState(new DownloadController.Completed());
					}
				});
	}

	private void pause() {
		downloadController.setState(new DownloadController.Paused());
		dispose(disposable);
	}

	private void installApk() {
		File[] files = rxDownload.getRealFiles(url);
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
}
