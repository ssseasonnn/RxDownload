package zlc.season.rxdownloadproject;

import android.app.Application;

import com.github.anrwatchdog.ANRWatchDog;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/10/27
 * Time: 13:29
 * FIXME
 */

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new ANRWatchDog().start();
    }


}
