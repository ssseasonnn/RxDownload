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

    private DownloadStatus mStatus;
    private Subscription mSubscription;

    public DownloadStatus getStatus() {
        return mStatus;
    }

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

    public void start(final Map<String, DownloadMission> nowDownloadMap,
                      final AtomicInteger count, final DataBaseHelper helper,
                      final Map<String, Subject<DownloadEvent, DownloadEvent>> subjectPool) {
        nowDownloadMap.put(url, this);
        count.incrementAndGet();
        final DownloadEventFactory eventFactory = DownloadEventFactory.getSingleton();
        mSubscription = rxDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .onBackpressureLatest()
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        helper.updateRecord(url, DownloadFlag.STARTED);
                    }

                    @Override
                    public void onCompleted() {
                        subjectPool.get(url).onNext(eventFactory.factory(url, DownloadFlag.COMPLETED, mStatus));

                        helper.updateRecord(url, DownloadFlag.COMPLETED);
                        count.decrementAndGet();
                        nowDownloadMap.remove(url);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("error", e);
                        subjectPool.get(url).onNext(eventFactory.factory(url, DownloadFlag.FAILED, mStatus, e));

                        helper.updateRecord(url, DownloadFlag.FAILED);
                        count.decrementAndGet();
                        nowDownloadMap.remove(url);
                    }

                    @Override
                    public void onNext(DownloadStatus status) {
                        subjectPool.get(url).onNext(eventFactory.factory(url, DownloadFlag.STARTED, status));
                        helper.updateRecord(url, status);
                        mStatus = status;
                    }
                });
    }

    public static class Builder {
        RxDownload rxDownload;
        String url;
        String saveName;
        String savePath;

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

        public DownloadMission build() {
            DownloadMission task = new DownloadMission();
            task.rxDownload = rxDownload;
            task.url = url;
            task.saveName = saveName;
            task.savePath = savePath;
            return task;
        }
    }
}
