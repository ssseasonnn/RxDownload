package zlc.season.rxdownload.java_demo;


import android.app.Application;

import zlc.season.rxdownload3.core.DownloadConfig;
import zlc.season.rxdownload3.extension.ApkInstallExtension;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        DownloadConfig.Builder builder = DownloadConfig.Builder.Companion.create(this)
                .enableDb(true)
                .enableService(true)
                .enableNotification(true)
                .addExtension(ApkInstallExtension.class);

        DownloadConfig.INSTANCE.init(builder);
    }
}
