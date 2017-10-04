package zlc.season.rxdownload.java_demo;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;
import zlc.season.rxdownload.java_demo.databinding.ActivityMainBinding;
import zlc.season.rxdownload.java_demo.databinding.ContentMainBinding;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mainBinding;
    private ContentMainBinding contentBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission(WRITE_EXTERNAL_STORAGE);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        contentBinding = mainBinding.contentMain;

        setSupportActionBar(mainBinding.toolbar);

        contentBinding.basicDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, BasicDownloadActivity.class));
            }
        });

        contentBinding.appMarket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AppListActivity.class));
            }
        });

    }

    private void requestPermission(String permission) {
        new RxPermissions(this)
                .request(permission)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (!aBoolean) {
                            finish();
                        }
                    }
                });
    }
}
