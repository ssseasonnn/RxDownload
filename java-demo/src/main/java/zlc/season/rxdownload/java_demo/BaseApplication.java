package zlc.season.rxdownload.java_demo;


import android.app.Application;

import zlc.season.rxdownload3.core.DownloadConfig;
import zlc.season.rxdownload3.extension.ApkInstallExtension;
import zlc.season.rxdownload3.extension.ApkOpenExtension;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        DownloadConfig.Builder builder = DownloadConfig.Builder.Companion.create(this)
                .enableDb(true)
                .setDbActor(new CustomSqliteActor(this))
                .enableService(true)
                .enableNotification(true)
                .addExtension(ApkInstallExtension.class)
                .addExtension(ApkOpenExtension.class);

        DownloadConfig.INSTANCE.init(builder);
    }
}
