package zlc.season.rxdownloadproject;

import android.app.Application;

import com.facebook.stetho.Stetho;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/23
 * FIXME
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
}
