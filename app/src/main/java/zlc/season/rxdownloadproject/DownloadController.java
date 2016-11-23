package zlc.season.rxdownloadproject;

import android.widget.Button;
import android.widget.TextView;

import zlc.season.rxdownload.entity.DownloadEvent;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/22
 * Time: 15:18
 * FIXME
 */
public class DownloadController {
    private TextView mStatus;
    private Button mAction;

    private DownloadState mState;

    public DownloadController(TextView status, Button action) {
        mStatus = status;
        mAction = action;
    }

    public void setState(DownloadState state) {
        mState = state;
        mState.setText(mStatus, mAction);
    }

    public void setEvent(DownloadEvent event) {
        if (event instanceof DownloadEvent.NormalEvent) {
            setState(new DownloadController.Normal());
        } else if (event instanceof DownloadEvent.WaitingEvent) {
            setState(new DownloadController.Waiting());
        } else if (event instanceof DownloadEvent.StartedEvent) {
            setState(new DownloadController.Started());
        } else if (event instanceof DownloadEvent.PausedEvent) {
            setState(new DownloadController.Paused());
        } else if (event instanceof DownloadEvent.CanceledEvent) {
            setState(new DownloadController.Canceled());
        } else if (event instanceof DownloadEvent.CompletedEvent) {
            setState(new DownloadController.Completed());
        } else if (event instanceof DownloadEvent.FailedEvent) {
            setState(new DownloadController.Failed());
        }
    }

    public void handleClick(Callback callback) {
        mState.handleClick(callback);
    }

    public interface Callback {
        void startDownload();

        void pauseDownload();

        void cancelDownload();

        void install();
    }

    static abstract class DownloadState {

        abstract void setText(TextView status, Button button);

        abstract void handleClick(Callback callback);
    }

    public static class Normal extends DownloadState {

        @Override
        void setText(TextView status, Button button) {
            button.setText("下载");
            status.setText("");
        }

        @Override
        void handleClick(Callback callback) {
            callback.startDownload();
        }
    }

    public static class Waiting extends DownloadState {
        @Override
        void setText(TextView status, Button button) {
            button.setText("取消");
            status.setText("等待中...");
        }

        @Override
        void handleClick(Callback callback) {
            callback.cancelDownload();
        }
    }

    public static class Started extends DownloadState {
        @Override
        void setText(TextView status, Button button) {
            button.setText("暂停");
            status.setText("下载中...");
        }

        @Override
        void handleClick(Callback callback) {
            callback.pauseDownload();
        }
    }

    public static class Paused extends DownloadState {
        @Override
        void setText(TextView status, Button button) {
            button.setText("继续");
            status.setText("已暂停");
        }

        @Override
        void handleClick(Callback callback) {
            callback.startDownload();
        }
    }

    public static class Failed extends DownloadState {
        @Override
        void setText(TextView status, Button button) {
            button.setText("继续");
            status.setText("下载失败");
        }

        @Override
        void handleClick(Callback callback) {
            callback.startDownload();
        }
    }

    public static class Canceled extends DownloadState {
        @Override
        void setText(TextView status, Button button) {
            button.setText("下载");
            status.setText("下载已取消");
        }

        @Override
        void handleClick(Callback callback) {
            callback.startDownload();
        }
    }

    public static class Completed extends DownloadState {
        @Override
        void setText(TextView status, Button button) {
            button.setText("安装");
            status.setText("下载已完成");
        }

        @Override
        void handleClick(Callback callback) {
            callback.install();
        }
    }
}
