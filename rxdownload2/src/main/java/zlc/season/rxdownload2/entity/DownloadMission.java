package zlc.season.rxdownload2.entity;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;

import static zlc.season.rxdownload2.entity.DownloadFlag.COMPLETED;
import static zlc.season.rxdownload2.entity.DownloadFlag.FAILED;
import static zlc.season.rxdownload2.entity.DownloadFlag.STARTED;

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
    private Disposable mDisposable;

    public Disposable getDisposable() {
        return mDisposable;
    }

    public DownloadStatus getStatus() {
        return mStatus;
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
            final Map<String, FlowableProcessor<DownloadEvent>> processorPool) {

        nowDownloadMap.put(url, this);
        count.incrementAndGet();

        final DownloadEventFactory eventFactory = DownloadEventFactory.getSingleton();

        rxDownload.download(url, saveName, savePath)
                  .subscribeOn(Schedulers.io())
                  .subscribe(new Observer<DownloadStatus>() {
                      @Override
                      public void onSubscribe(Disposable d) {
                          helper.updateRecord(url, STARTED);
                          mDisposable = d;
                      }

                      @Override
                      public void onNext(DownloadStatus value) {
                          processorPool.get(url).onNext(eventFactory.started(url, value));
                          helper.updateRecord(url, value);
                          mStatus = value;
                      }

                      @Override
                      public void onError(Throwable e) {
                          processorPool.get(url).onNext(eventFactory.failed(url, mStatus, e));
                          helper.updateRecord(url, FAILED);
                          count.decrementAndGet();
                          nowDownloadMap.remove(url);
                      }

                      @Override
                      public void onComplete() {
                          processorPool.get(url).onNext(eventFactory.completed(url, mStatus));
                          helper.updateRecord(url, COMPLETED);
                          count.decrementAndGet();
                          nowDownloadMap.remove(url);
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
