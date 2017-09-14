// IDownloadCallback.aidl
package zlc.season.rxdownload3;

// Declare any non-default types here with import statements
import zlc.season.rxdownload3.core.Status;

interface IDownloadCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
     void onUpdate(in Status status);
}
