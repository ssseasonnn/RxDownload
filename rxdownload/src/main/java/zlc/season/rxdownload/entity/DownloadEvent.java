package zlc.season.rxdownload.entity;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/22
 * Time: 10:49
 * FIXME
 */
public class DownloadEvent {
    public DownloadStatus downloadStatus;
    public int flag;

    public static DownloadEvent createEvent(int flag, DownloadStatus status) {
        switch (flag) {
            case NormalEvent.flag:
                return EventHolder.NORMAL.setDownloadStatus(status);
            case WaitingEvent.flag:
                return EventHolder.WAITING.setDownloadStatus(status);
            case StartedEvent.flag:
                return EventHolder.STARTED.setDownloadStatus(status);
            case PausedEvent.flag:
                return EventHolder.PAUSED.setDownloadStatus(status);
            case CanceledEvent.flag:
                return EventHolder.CANCELED.setDownloadStatus(status);
            case CompletedEvent.flag:
                return EventHolder.COMPLETED.setDownloadStatus(status);
            case FailedEvent.flag:
                return EventHolder.FAILED.setDownloadStatus(status);
            case InstallEvent.flag:
                return EventHolder.INSTALL.setDownloadStatus(status);
            case InstalledEvent.flag:
                return EventHolder.INSTALLED.setDownloadStatus(status);
            default:
                return EventHolder.NORMAL.setDownloadStatus(status);
        }
    }

    protected DownloadEvent setDownloadStatus(DownloadStatus status) {
        this.downloadStatus = status;
        return this;
    }

    public static class EventHolder {
        public static final DownloadEvent NORMAL = new NormalEvent();
        public static final DownloadEvent WAITING = new WaitingEvent();
        public static final DownloadEvent STARTED = new StartedEvent();
        public static final DownloadEvent PAUSED = new PausedEvent();
        public static final DownloadEvent CANCELED = new CanceledEvent();
        public static final DownloadEvent COMPLETED = new CompletedEvent();
        public static final DownloadEvent FAILED = new FailedEvent();
        public static final DownloadEvent INSTALL = new InstallEvent();
        public static final DownloadEvent INSTALLED = new InstalledEvent();
    }

    private static class NormalEvent extends DownloadEvent {
        public static final int flag = 9990;   //下载未开始
    }

    private static class WaitingEvent extends DownloadEvent {
        public static final int flag = 9991;   //等待下载
    }

    private static class StartedEvent extends DownloadEvent {
        public static final int flag = 9992;  //下载已开始
    }

    private static class PausedEvent extends DownloadEvent {
        public static final int flag = 9993;   //下载已暂停
    }

    private static class CanceledEvent extends DownloadEvent {
        public static final int flag = 9994; //下载已取消
    }

    private static class CompletedEvent extends DownloadEvent {
        public static final int flag = 9995;//下载已完成
    }

    private static class FailedEvent extends DownloadEvent {
        public static final int flag = 9996;   //下载已失败
    }

    private static class InstallEvent extends DownloadEvent {
        public static final int flag = 9997;
    }

    private static class InstalledEvent extends DownloadEvent {
        public static final int flag = 9998;
    }
}
