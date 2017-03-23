package zlc.season.rxdownloadproject.activity;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownloadproject.Constants;
import zlc.season.rxdownloadproject.R;
import zlc.season.rxdownloadproject.databinding.ActivityMultiMissionDownloadBinding;
import zlc.season.rxdownloadproject.model.MutiModel;

import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.log;

public class MultiMissionDownloadActivity extends AppCompatActivity {

	private static final String missionId = "testMissionId";
	//
	private String url1 = Constants.URL1;
	private String url2 = Constants.URL2;
	private String url3 = Constants.URL3;
	private RxDownload rxDownload;
	private Disposable disposable1,
			disposable2,
			disposable3;
	private MutiModel mutiModel = null;
	private ActivityMultiMissionDownloadBinding binding = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// initData
		mutiModel = new MutiModel();
		// initView
		binding = DataBindingUtil.setContentView(this, R.layout.activity_multi_mission_download);
		binding.setItem(mutiModel);
		binding.contentMultiMissionDownload.setPresenter(new Presenter());
		setSupportActionBar(binding.toolbar);
		//
		rxDownload = RxDownload.getInstance(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		//
		startMultiMission();
		//
		disposable1 = rxDownload.receiveDownloadStatus(url1)
				.subscribe(new Consumer<DownloadEvent>() {
					@Override
					public void accept(DownloadEvent downloadEvent) throws Exception {
						int flag = downloadEvent.getFlag();
						switch (flag) {
							case DownloadFlag.NORMAL:
								binding.contentMultiMissionDownload.control1.setVisibility(View.GONE);
								break;
							case DownloadFlag.WAITING:
								binding.contentMultiMissionDownload.control1.setVisibility(View.VISIBLE);
								binding.contentMultiMissionDownload.control1.setText("等待中");
								break;
							case DownloadFlag.STARTED:
								binding.contentMultiMissionDownload.control1.setText("下载中");
								break;
							case DownloadFlag.PAUSED:
								binding.contentMultiMissionDownload.control1.setText("已暂停");
								break;
							case DownloadFlag.COMPLETED:
								binding.contentMultiMissionDownload.control1.setText("已完成");
								break;
							case DownloadFlag.FAILED:
								Throwable throwable = downloadEvent.getError();
								log(throwable);
								binding.contentMultiMissionDownload.control1.setText("失败");
								break;
						}
						DownloadStatus status = downloadEvent.getDownloadStatus();
						binding.contentMultiMissionDownload.progress1.setProgress(status.getPercentNumber());
					}
				});
		//
		disposable2 = rxDownload.receiveDownloadStatus(url2)
				.subscribe(new Consumer<DownloadEvent>() {
					@Override
					public void accept(DownloadEvent downloadEvent) throws Exception {
						int flag = downloadEvent.getFlag();
						switch (flag) {
							case DownloadFlag.NORMAL:
								binding.contentMultiMissionDownload.control2.setVisibility(View.GONE);
								break;
							case DownloadFlag.WAITING:
								binding.contentMultiMissionDownload.control2.setVisibility(View.VISIBLE);
								binding.contentMultiMissionDownload.control2.setText("等待中");
								break;
							case DownloadFlag.STARTED:
								binding.contentMultiMissionDownload.control2.setText("下载中");
								break;
							case DownloadFlag.PAUSED:
								binding.contentMultiMissionDownload.control2.setText("已暂停");
								break;
							case DownloadFlag.COMPLETED:
								binding.contentMultiMissionDownload.control2.setText("已完成");
								break;
							case DownloadFlag.FAILED:
								Throwable throwable = downloadEvent.getError();
								log(throwable);
								binding.contentMultiMissionDownload.control2.setText("失败");
								break;
						}
						DownloadStatus status = downloadEvent.getDownloadStatus();
						binding.contentMultiMissionDownload.progress2.setProgress(status.getPercentNumber());
					}
				});
		//
		disposable3 = rxDownload.receiveDownloadStatus(url3)
				.subscribe(new Consumer<DownloadEvent>() {
					@Override
					public void accept(DownloadEvent downloadEvent) throws Exception {
						int flag = downloadEvent.getFlag();
						switch (flag) {
							case DownloadFlag.NORMAL:
								binding.contentMultiMissionDownload.control3.setVisibility(View.GONE);
								break;
							case DownloadFlag.WAITING:
								binding.contentMultiMissionDownload.control3.setVisibility(View.VISIBLE);
								binding.contentMultiMissionDownload.control3.setText("等待中");
								break;
							case DownloadFlag.STARTED:
								binding.contentMultiMissionDownload.control3.setText("下载中");
								break;
							case DownloadFlag.PAUSED:
								binding.contentMultiMissionDownload.control3.setText("已暂停");
								break;
							case DownloadFlag.COMPLETED:
								binding.contentMultiMissionDownload.control3.setText("已完成");
								break;
							case DownloadFlag.FAILED:
								Throwable throwable = downloadEvent.getError();
								log(throwable);
								binding.contentMultiMissionDownload.control3.setText("失败");
								break;
						}
						DownloadStatus status = downloadEvent.getDownloadStatus();
						binding.contentMultiMissionDownload.progress3.setProgress(status.getPercentNumber());
					}
				});
	}

	@Override
	protected void onPause() {
		super.onPause();
		dispose(disposable1);
		dispose(disposable2);
		dispose(disposable3);
	}

	private void startMultiMission() {
		rxDownload.serviceMultiDownload(missionId, url1, url2, url3)
				.subscribe(new Consumer<Object>() {
					@Override
					public void accept(Object o) throws Exception {
						Toast.makeText(MultiMissionDownloadActivity.this, "开始", Toast.LENGTH_SHORT).show();
					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {
						log(throwable);
					}
				});
	}

	public class Presenter {
		public void onClick(View view) {
			switch (view.getId()) {
				case R.id.start:
					rxDownload.startAll(missionId)
							.subscribe(new Consumer<Object>() {
								@Override
								public void accept(Object o) throws Exception {
									Toast.makeText(MultiMissionDownloadActivity.this, "全部开始", Toast
											.LENGTH_SHORT).show();
								}
							}, new Consumer<Throwable>() {
								@Override
								public void accept(Throwable throwable) throws Exception {
									log(throwable);
								}
							});
					break;
				case R.id.pause:
					rxDownload.pauseAll(missionId)
							.subscribe(new Consumer<Object>() {
								@Override
								public void accept(Object o) throws Exception {
									Toast.makeText(MultiMissionDownloadActivity.this, "全部暂停", Toast
											.LENGTH_SHORT).show();
								}
							}, new Consumer<Throwable>() {
								@Override
								public void accept(Throwable throwable) throws Exception {
									log(throwable);
								}
							});
					break;
				case R.id.delete:
					rxDownload.deleteAll(missionId, true)
							.subscribe(new Consumer<Object>() {
								@Override
								public void accept(Object o) throws Exception {
									Toast.makeText(MultiMissionDownloadActivity.this, "删除成功", Toast
											.LENGTH_SHORT).show();
								}
							}, new Consumer<Throwable>() {
								@Override
								public void accept(Throwable throwable) throws Exception {
									log(throwable);
								}
							});
			}
		}
	}
}