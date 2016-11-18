package zlc.season.rxdownload;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import static zlc.season.rxdownload.DownloadFlag.COMPLETED;
import static zlc.season.rxdownload.DownloadFlag.FAILED;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/18
 * Time: 11:38
 * FIXME
 */
class DownloadTask {
    String TAG = "tag";
    private RxDownload rxDownload;
    private String url;
    private String saveName;
    private String savePath;
    private String name;
    private String image;


    void start(final DataBaseHelper db, final AtomicInteger currentNumber,
               Map<String, Subject<DownloadStatus, DownloadStatus>> subjectPool,
               final Map<String, Subscription> subscriptionPool) {

        currentNumber.incrementAndGet();

        Log.d(TAG, "task download");

        if (subscriptionPool.get(url) != null) {
            Log.w(TAG, "This url download task already exists! So do nothing.");
            return;
        }

        if (db.recordNotExists(url)) {
            db.insertRecord(url, saveName, rxDownload.getFileSavePaths(savePath)[0], name, image);
        }

        final Subject<DownloadStatus, DownloadStatus> finalSubject;
        if (subjectPool.get(url) == null) {
            finalSubject = PublishSubject.create();
            subjectPool.put(url, finalSubject);
        } else {
            finalSubject = subjectPool.get(url);
        }

        Subscription temp = rxDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .sample(500, TimeUnit.MILLISECONDS)
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        finalSubject.onCompleted();
                        Utils.unSubscribe(subscriptionPool.get(url));
                        subscriptionPool.remove(url);
                        db.updateRecord(url, COMPLETED);
                        currentNumber.decrementAndGet();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        finalSubject.onError(e);
                        Utils.unSubscribe(subscriptionPool.get(url));
                        subscriptionPool.remove(url);
                        db.updateRecord(url, FAILED);
                        currentNumber.decrementAndGet();
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        finalSubject.onNext(status);
                        db.updateRecord(url, status);
                    }
                });
        subscriptionPool.put(url, temp);
    }

    static class Builder {
        RxDownload rxDownload;
        String url;
        String saveName;
        String savePath;
        String name;
        String image;

        Builder setRxDownload(RxDownload rxDownload) {
            this.rxDownload = rxDownload;
            return this;
        }

        Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        Builder setSaveName(String saveName) {
            this.saveName = saveName;
            return this;
        }

        Builder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        Builder setName(String name) {
            this.name = name;
            return this;
        }

        Builder setImage(String image) {
            this.image = image;
            return this;
        }

        DownloadTask build() {
            DownloadTask task = new DownloadTask();
            task.rxDownload = rxDownload;
            task.url = url;
            task.saveName = saveName;
            task.savePath = savePath;
            task.name = name;
            task.image = image;
            return task;
        }
    }
}
