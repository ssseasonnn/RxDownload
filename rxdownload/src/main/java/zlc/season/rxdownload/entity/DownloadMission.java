package zlc.season.rxdownload.entity;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.Subject;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownload.db.DataBaseHelper;
import zlc.season.rxdownload.function.Utils;

import static zlc.season.rxdownload.entity.DownloadFlag.COMPLETED;
import static zlc.season.rxdownload.entity.DownloadFlag.FAILED;
import static zlc.season.rxdownload.entity.DownloadFlag.STARTED;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/18
 * Time: 11:38
 * FIXME
 */
public class DownloadMission {
    public boolean canceled = false;
    private RxDownload rxDownload;
    private String url;
    private String saveName;
    private String savePath;
    private String name;
    private String image;

    private Subscription mSubscription;

    public Subscription getSubscription() {
        return mSubscription;
    }

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

    public void start(final Map<String, DownloadMission> nowDownloadMap,
                      final Subject<DownloadStatus, DownloadStatus> subject,
                      final AtomicInteger count, final DataBaseHelper helper) {
        nowDownloadMap.put(url, this);
        count.incrementAndGet();
        mSubscription = rxDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .onBackpressureLatest()
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        helper.updateRecord(url, STARTED);
                    }

                    @Override
                    public void onCompleted() {
                        subject.onCompleted();
                        helper.updateRecord(url, COMPLETED);
                        count.decrementAndGet();
                        nowDownloadMap.remove(url);
                        Utils.unSubscribe(mSubscription);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        subject.onError(e);
                        helper.updateRecord(url, FAILED);
                        count.decrementAndGet();
                        nowDownloadMap.remove(url);
                        Utils.unSubscribe(mSubscription);
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        subject.onNext(status);
                        helper.updateRecord(url, status);
                    }
                });
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
