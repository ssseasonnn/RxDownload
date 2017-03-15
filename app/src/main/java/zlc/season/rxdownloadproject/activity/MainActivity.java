package zlc.season.rxdownloadproject.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.function.Utils;
import zlc.season.rxdownloadproject.R;
import zlc.season.rxdownloadproject.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
		binding.contentMain.setPresenter(new Presenter());
		setSupportActionBar(binding.toolbar);
		//
		Utils.setDebug(true);
		RxDownload.getInstance(this)
				.maxDownloadNumber(2)
				.maxThread(3);
	}

	public class Presenter {
		public void onClick(View view) {
			switch (view.getId()) {
				case R.id.basic_download:
					startActivity(new Intent(MainActivity.this, BasicDownloadActivity.class));
					break;
				case R.id.service_download:
					startActivity(new Intent(MainActivity.this, ServiceDownloadActivity.class));
					break;
				case R.id.multi_mission:
					startActivity(new Intent(MainActivity.this, MultiMissionDownloadActivity.class));
					break;
				case R.id.app_market:
					startActivity(new Intent(MainActivity.this, AppMarketActivity.class));
					break;
			}
		}
	}
}
