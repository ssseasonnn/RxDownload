// IDownloadService.aidl
package zlc.season.rxdownload3;

// Declare any non-default types here with import statements
import zlc.season.rxdownload3.core.Mission;
import zlc.season.rxdownload3.IDownloadCallback;

interface IDownloadService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void registerDownloadCallback(in IDownloadCallback callback, in Mission mission);

    void unregisterDownloadCallback(in IDownloadCallback callback);

    void start(in Mission mission);
}
