package zlc.season.rxdownload2.entity;

import java.util.List;
import java.util.concurrent.Semaphore;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.db.DataBaseHelper;

import static zlc.season.rxdownload2.function.Constant.ACQUIRE_SUCCESS;
import static zlc.season.rxdownload2.function.Constant.ACQUIRE_SURPLUS_SEMAPHORE;
import static zlc.season.rxdownload2.function.Constant.RELEASE_SURPLUS_SEMAPHORE;
import static zlc.season.rxdownload2.function.Constant.TRY_TO_ACQUIRE_SEMAPHORE;
import static zlc.season.rxdownload2.function.DownloadEventFactory.completed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.failed;
import static zlc.season.rxdownload2.function.DownloadEventFactory.started;
import static zlc.season.rxdownload2.function.Utils.dispose;
import static zlc.season.rxdownload2.function.Utils.formatStr;
import static zlc.season.rxdownload2.function.Utils.log;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/18
 * Time: 11:38
 * FIXME
 */
public abstract class DownloadMission {
    protected boolean canceled = false;

    protected DownloadStatus status;

    protected RxDownload rxdownload;
    protected Disposable disposable;

    public Disposable getDisposable() {
        return disposable;
    }

    public void markCanceled() {
        this.canceled = true;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public abstract String getKey();

    public abstract void insertOrUpdate(DataBaseHelper dataBaseHelper);

    public abstract void start(final Semaphore semaphore, final FlowableProcessor<DownloadEvent> processor);

    public static class SingleMission extends DownloadMission {
        private DownloadBean bean;

        public SingleMission(DownloadBean bean, RxDownload rxdownload) {
            this.rxdownload = rxdownload;
            this.bean = bean;
        }

        @Override
        public String getKey() {
            return bean.getUrl();
        }

        @Override
        public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
            if (dataBaseHelper.recordNotExists(getKey())) {
                dataBaseHelper.insertRecord(bean, DownloadFlag.WAITING, null);
            } else {
                dataBaseHelper.updateFlag(getKey(), DownloadFlag.WAITING);
            }
        }

        @Override
        public void start(final Semaphore semaphore, final FlowableProcessor<DownloadEvent> processor) {
            disposable = rxdownload.download(bean)
                    .subscribeOn(Schedulers.io())
                    .doOnLifecycle(new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable disposable) throws Exception {
                            log(TRY_TO_ACQUIRE_SEMAPHORE);
                            semaphore.acquire();
                            log(ACQUIRE_SUCCESS);
                            log(formatStr(ACQUIRE_SURPLUS_SEMAPHORE, semaphore.availablePermits()));
                        }
                    }, new Action() {
                        @Override
                        public void run() throws Exception {
                            log(formatStr(RELEASE_SURPLUS_SEMAPHORE, semaphore.availablePermits() + 1));
                            semaphore.release();
                        }
                    })
                    .doOnSubscribe(new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable disposable) throws Exception {
                            if (canceled) {
                                dispose(disposable);
                            }
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


    public class MultiMission extends DownloadMission {
        private List<DownloadBean> missions;
        private List<Disposable> disposables;
        private List<DownloadStatus> statuses;

        private String group;

        public String getKey() {
            return group;
        }

        @Override
        public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
            for (DownloadBean each : missions) {
                if (dataBaseHelper.recordNotExists(each.getUrl(), group)) {
                    dataBaseHelper.insertRecord(each, DownloadFlag.WAITING, group);
                } else {
                    dataBaseHelper.updateFlag(each.getUrl(), DownloadFlag.WAITING);
                }
            }
        }

        @Override
        public void start(Semaphore semaphore, FlowableProcessor<DownloadEvent> processor) {
            for (DownloadBean each : missions) {
                Disposable disposable = rxdownload.download(each)
                        .subscribeOn(Schedulers.io())
                        .subscribe();
                disposables.add(disposable);
            }
        }
    }
}
