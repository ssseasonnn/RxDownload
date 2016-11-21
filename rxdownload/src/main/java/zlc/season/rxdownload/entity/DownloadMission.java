package zlc.season.rxdownload.entity;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownload.db.DataBaseHelper;
import zlc.season.rxdownload.util.Mission;
import zlc.season.rxdownload.util.Utils;

import static zlc.season.rxdownload.entity.DownloadFlag.CANCELED;
import static zlc.season.rxdownload.entity.DownloadFlag.COMPLETED;
import static zlc.season.rxdownload.entity.DownloadFlag.FAILED;
import static zlc.season.rxdownload.entity.DownloadFlag.PAUSED;
import static zlc.season.rxdownload.entity.DownloadFlag.STARTED;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/18
 * Time: 11:38
 * FIXME
 */
public class DownloadMission implements Mission {
    private RxDownload rxDownload;
    private String url;
    private String saveName;
    private String savePath;
    private String name;
    private String image;

    private Subscription mSubscription;
    private AtomicInteger mCount;
    private DataBaseHelper mDb;
    private Subject<DownloadStatus, DownloadStatus> mRealSubject;

    public String getUrl() {
        return url;
    }

    public String getSaveName() {
        return saveName;
    }

    public String getSavePath() {
        return savePath;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    @Override
    public void start() {
        mCount.incrementAndGet();
        mSubscription = rxDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .onBackpressureLatest()
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        mDb.updateRecord(url, STARTED);
                    }

                    @Override
                    public void onCompleted() {
                        mRealSubject.onCompleted();
                        mDb.updateRecord(url, COMPLETED);
                        mCount.decrementAndGet();
                        Utils.unSubscribe(mSubscription);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        mRealSubject.onError(e);
                        mDb.updateRecord(url, FAILED);
                        mCount.decrementAndGet();
                        Utils.unSubscribe(mSubscription);
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        mRealSubject.onNext(status);
                        mDb.updateRecord(url, status);
                    }
                });
    }


    @Override
    public void init(Map<String, Subject<DownloadStatus, DownloadStatus>> subjectPool, AtomicInteger count,
                     DataBaseHelper db) {
        this.mCount = count;
        this.mDb = db;

        if (subjectPool.get(url) == null) {
            mRealSubject = PublishSubject.create();
            subjectPool.put(url, mRealSubject);
        } else {
            mRealSubject = subjectPool.get(url);
        }
    }


    @Override
    public void pause() {
        Utils.unSubscribe(mSubscription);
        mDb.updateRecord(url, PAUSED);
        mCount.decrementAndGet();
    }


    public void delete() {
        Utils.unSubscribe(mSubscription);
        mDb.deleteRecord(url);
        mCount.decrementAndGet();
    }

    @Override
    public void cancel() {
        Utils.unSubscribe(mSubscription);
        mDb.updateRecord(url, CANCELED);
        mCount.decrementAndGet();
    }

    public static class Builder {
        RxDownload rxDownload;
        String url;
        String saveName;
        String savePath;
        String name;
        String image;

        public Builder setRxDownload(RxDownload rxDownload) {
            this.rxDownload = rxDownload;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setSaveName(String saveName) {
            this.saveName = saveName;
            return this;
        }

        public Builder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setImage(String image) {
            this.image = image;
            return this;
        }

        public DownloadMission build() {
            DownloadMission task = new DownloadMission();
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
