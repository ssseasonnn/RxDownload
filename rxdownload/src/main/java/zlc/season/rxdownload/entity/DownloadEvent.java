package zlc.season.rxdownload.entity;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/22
 * Time: 10:49
 * FIXME
 */
public class DownloadEvent {
    public int flag;
    public DownloadStatus downloadStatus;

    public static DownloadEvent createEvent(int flag, DownloadStatus status) {
        switch (flag) {
            case FlagHolder.NORMAL:
                return EventHolder.NORMAL.set(status);
            case FlagHolder.WAITING:
                return EventHolder.WAITING.set(status);
            case FlagHolder.STARTED:
                return EventHolder.STARTED.set(status);
            case FlagHolder.PAUSED:
                return EventHolder.PAUSED.set(status);
            case FlagHolder.CANCELED:
                return EventHolder.CANCELED.set(status);
            case FlagHolder.COMPLETED:
                return EventHolder.COMPLETED.set(status);
            case FlagHolder.FAILED:
                return EventHolder.FAILED.set(status);
            case FlagHolder.INSTALL:
                return EventHolder.INSTALL.set(status);
            case FlagHolder.INSTALLED:
                return EventHolder.INSTALLED.set(status);
            default:
                return EventHolder.NORMAL.set(status);
        }
    }

    protected DownloadEvent set(DownloadStatus status) {
        this.downloadStatus = status;
        return this;
    }

    public static class FlagHolder {
        public static final int NORMAL = 9990;
        public static final int WAITING = 9991;
        public static final int STARTED = 9992;
        public static final int PAUSED = 9993;
        public static final int CANCELED = 9994;
        public static final int COMPLETED = 9995;
        public static final int FAILED = 9996;
        public static final int INSTALL = 9997;
        public static final int INSTALLED = 9998;
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

    public static class NormalEvent extends DownloadEvent {
        @Override
        public NormalEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.NORMAL;
            return this;
        }
    }

    public static class WaitingEvent extends DownloadEvent {
        @Override
        public WaitingEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.WAITING;
            return this;
        }
    }

    public static class StartedEvent extends DownloadEvent {
        @Override
        public StartedEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.STARTED;
            return this;
        }
    }

    public static class PausedEvent extends DownloadEvent {
        @Override
        public PausedEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.PAUSED;
            return this;
        }
    }

    public static class CanceledEvent extends DownloadEvent {
        @Override
        public CanceledEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.CANCELED;
            return this;
        }
    }

    public static class CompletedEvent extends DownloadEvent {
        @Override
        public CompletedEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.COMPLETED;
            return this;
        }
    }

    public static class FailedEvent extends DownloadEvent {
        @Override
        public FailedEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.FAILED;
            return this;
        }
    }

    public static class InstallEvent extends DownloadEvent {
        @Override
        public InstallEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.INSTALL;
            return this;
        }
    }

    public static class InstalledEvent extends DownloadEvent {
        @Override
        public InstalledEvent set(DownloadStatus status) {
            super.set(status);
            flag = FlagHolder.INSTALLED;
            return this;
        }
    }
}
