package zlc.season.rxdownloadproject;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;

import static zlc.season.rxdownloadproject.R.id.fab;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.download)
    Button mDownload;
    @BindView(R.id.content_main)
    RelativeLayout mContentMain;
    @BindView(fab)
    FloatingActionButton mFab;
    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;
    @BindView(R.id.pause)
    Button mPause;
    @BindView(R.id.cancel)
    Button mCancel;


    private Subscription mSubscription;
    private String weixin = "http://dldir1.qq.com/weixin/android/weixin6327android880.apk";
    private String android = "http://www.taxiaides.com/xyyc/file/version/android";
    private Subscription mSubscription1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.download, fab, R.id.pause, R.id.cancel})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.download:
                mSubscription1 = RxDownload.getInstance()
                        .download(android, null, null, true)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<DownloadStatus>() {

                            @Override
                            public void onCompleted() {
                                if (mSubscription1 != null && !mSubscription1.isUnsubscribed()) {
                                    mSubscription1.unsubscribe();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.w("Error", e);
                            }

                            @Override
                            public void onNext(DownloadStatus result) {
                                mProgressBar.setMax((int) result.totalSize);
                                mProgressBar.setProgress((int) result.downloadSize);
                            }
                        });
                break;
            case fab:
                mSubscription = RxDownload.getInstance()
                        .maxThread(1)
                        .download(weixin, null, null, true)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<DownloadStatus>() {

                            @Override
                            public void onCompleted() {
                                if (mSubscription != null && !mSubscription.isUnsubscribed()) {
                                    mSubscription.unsubscribe();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.w("Error", e);
                            }

                            @Override
                            public void onNext(DownloadStatus result) {
                                Log.d("on next",Thread.currentThread().getName());
                                mProgressBar.setMax((int) result.totalSize);
                                mProgressBar.setProgress((int) result.downloadSize);
                            }
                        });
                break;

            case R.id.pause:

                break;
            case R.id.cancel:
                mSubscription.unsubscribe();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
////                    .detectDiskReads()
////                    .detectDiskWrites()
////                    .detectNetwork()   // or .detectAll() for all detectable problems
//                    .detectAll()
//                    .penaltyLog()
//                    .penaltyDialog()
//                    .build());
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
////                    .detectLeakedSqlLiteObjects()
////                    .detectLeakedClosableObjects()
//                    .detectAll()
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build());
//        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

//        mSubscription = RxDownload.getInstance()
//                .maxThread(1)
//                .download(weixin, null, null, true)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Subscriber<DownloadStatus>() {
//
//                    @Override
//                    public void onCompleted() {
//                        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
//                            mSubscription.unsubscribe();
//                        }
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        Log.w("Error", e);
//                    }
//
//                    @Override
//                    public void onNext(DownloadStatus result) {
//                        mProgressBar.setMax((int) result.totalSize);
//                        mProgressBar.setProgress((int) result.downloadSize);
//                    }
//                });
    }
}
