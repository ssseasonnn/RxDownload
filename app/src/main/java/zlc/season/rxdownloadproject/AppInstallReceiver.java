package zlc.season.rxdownloadproject;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class AppInstallReceiver extends BroadcastReceiver {

    private String installPackageName;
    private ApkInstallCallback callback;

    public AppInstallReceiver(String installPackageName, ApkInstallCallback callback) {
        this.installPackageName = installPackageName;
        this.callback = callback;
    }

    public AppInstallReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("haha");
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        Uri uri = intent.getData();
        if (uri == null) {
            return;
        }
        String packageName = uri.getEncodedSchemeSpecificPart();
        System.out.println(packageName);
        System.out.println(installPackageName);
        if (installPackageName.equals(packageName)) {
            if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                callback.added();
            }

            if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                callback.removed();
            }

            if (action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
                callback.replaced();
            }
        }
    }

    public interface ApkInstallCallback {
        void added();

        void removed();

        void replaced();
    }
}
