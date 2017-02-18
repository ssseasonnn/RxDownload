package zlc.season.rxdownload2.entity;

import java.util.concurrent.Semaphore;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;

import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.started;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/18
 * Time: 11:38
 * FIXME
 */
public class DownloadMission {
    private boolean canceled = false;

    private DownloadBean bean;

    private RxDownload rxdownload;
    private DownloadStatus status;
    private Disposable disposable;

    public DownloadMission(DownloadBean bean, RxDownload rxdownload) {
        this.bean = bean;
        this.rxdownload = rxdownload;
    }

    public Disposable getDisposable() {
        return disposable;
    }

    public void markCanceled() {
        this.canceled = true;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public DownloadBean getBean() {
        return bean;
    }

    public String getUrl() {
        return bean.getUrl();
    }

    public void start(final Semaphore semaphore, final FlowableProcessor<DownloadEvent> processor)
            throws InterruptedException {

        if (canceled) return;
        semaphore.acquire();

        disposable = rxdownload.download(bean)
                .subscribeOn(Schedulers.io())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        semaphore.release();
                    }
                })
                .subscribe(new Consumer<DownloadStatus>() {
                    @Override
                    public void accept(DownloadStatus value) throws Exception {
                        processor.onNext(started(value));
                        status = value;
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        processor.onNext(failed(status, throwable));
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        processor.onNext(completed(status));
                    }
                });
    }
}
