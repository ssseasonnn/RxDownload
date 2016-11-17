package zlc.season.rxdownloadproject;

import android.widget.Button;
import android.widget.TextView;

import zlc.season.rxdownload.DownloadFlag;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/15
 * Time: 15:26
 * 下载控制
 */
public class DownloadController {
    private DownloadState state;

    private TextView mStatusText;
    private Button mActionButton;

    public DownloadController(TextView statusText, Button actionButton) {
        mStatusText = statusText;
        mActionButton = actionButton;
    }

    /**
     * 设置并显示当前状态
     *
     * @param flag 状态标识
     */
    public void setStateAndDisplay(int flag) {
        switch (flag) {
            case DownloadFlag.NORMAL:
                this.state = new NormalState(this);
                break;
            case DownloadFlag.STARTED:
                this.state = new StartedState(this);
                break;
            case DownloadFlag.PAUSED:
                this.state = new PausedState(this);
                break;
            case DownloadFlag.FAILED:
                this.state = new FailedState(this);
                break;
            case DownloadFlag.CANCELED:
                this.state = new CanceledState(this);
                break;
            case DownloadFlag.COMPLETED:
                this.state = new CompletedState(this);
                break;
            case DownloadFlag.INSTALL:
                this.state = new InstallState(this);
                break;
        }

        displayNowState();
    }

    /**
     * 处理点击事件
     *
     * @param callback Callback
     */
    public void performClick(Callback callback) {
        state.handleClickEvent(callback);
    }

    private void displayNowState() {
        state.displayNowState(mStatusText, mActionButton);
    }

    /**
     * 状态处理回调
     */
    public interface Callback {

        void startDownload();

        void pauseDownload();

        void install();
    }

    static abstract class DownloadState {
        DownloadController mContext;

        DownloadState(DownloadController context) {
            mContext = context;
        }

        abstract void displayNowState(TextView status, Button action);

        abstract void handleClickEvent(Callback callback);

    }

    private static class NormalState extends DownloadState {

        NormalState(DownloadController context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            if (status != null) status.setText("");
            if (action != null) action.setText("下载");
        }

        @Override
        void handleClickEvent(Callback callback) {
            callback.startDownload();
        }
    }

    private static class StartedState extends DownloadState {

        StartedState(DownloadController context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            if (status != null) status.setText("正在下载...");
            if (action != null) action.setText("暂停");
        }

        @Override
        void handleClickEvent(Callback callback) {
            callback.pauseDownload();
        }
    }

    private static class PausedState extends DownloadState {

        PausedState(DownloadController context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            if (status != null) status.setText("下载已暂停");
            if (action != null) action.setText("继续");
        }

        @Override
        void handleClickEvent(Callback callback) {
            callback.startDownload();
        }
    }

    private static class FailedState extends DownloadState {

        FailedState(DownloadController context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            if (status != null) status.setText("下载失败");
            if (action != null) action.setText("下载");
        }

        @Override
        void handleClickEvent(Callback callback) {
            callback.startDownload();
        }
    }

    private static class CanceledState extends DownloadState {

        CanceledState(DownloadController context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            if (status != null) status.setText("下载已取消");
            if (action != null) action.setText("下载");
        }

        @Override
        void handleClickEvent(Callback callback) {
            callback.startDownload();
        }
    }

    private static class CompletedState extends DownloadState {

        CompletedState(DownloadController context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            if (status != null) status.setText("下载已完成");
            if (action != null) action.setText("安装");
        }

        @Override
        void handleClickEvent(Callback callback) {
            callback.install();
        }
    }

    private static class InstallState extends DownloadState {

        InstallState(DownloadController context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            if (status != null) status.setText("正在安装...");
            if (action != null) action.setText("安装中");
        }

        @Override
        void handleClickEvent(Callback callback) {
            //doNothing..
        }
    }
}

