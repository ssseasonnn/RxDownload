package zlc.season.rxdownloadproject;

import android.widget.Button;
import android.widget.TextView;

import zlc.season.rxdownload.DownloadRecord;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/15
 * Time: 15:26
 * 状态模式
 */
public class DownloadStateContext {
    private DownloadState state;

    private TextView mStatusText;
    private Button mActionButton;

    public DownloadStateContext(TextView statusText, Button actionButton) {
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
            case DownloadRecord.FLAG_NORMAL:
                this.state = new NormalState(this);
                break;
            case DownloadRecord.FLAG_STARTED:
                this.state = new StartedState(this);
                break;
            case DownloadRecord.FLAG_PAUSED:
                this.state = new PausedState(this);
                break;
            case DownloadRecord.FLAG_FAILED:
                this.state = new FailedState(this);
                break;
            case DownloadRecord.FLAG_CANCELED:
                this.state = new CanceledState(this);
                break;
            case DownloadRecord.FLAG_COMPLETED:
                this.state = new CompletedState(this);
                break;
            case DownloadRecord.FLAG_INSTALL:
                this.state = new InstallState(this);
                break;
        }

        displayNowState();
    }

    /**
     * 处理点击事件,切换到下一状态并显示
     *
     * @param callback Callback
     */
    public void performClick(Callback callback) {
        state.handle(mStatusText, mActionButton, callback);
    }

    private void displayNowState() {
        state.displayNowState(mStatusText, mActionButton);
    }

    private void setState(DownloadState state) {
        this.state = state;
    }

    /**
     * 状态处理回调
     */
    public interface Callback {

        void startDownload();

        void cancelDownload();

        void pauseDownload();

        void install();
    }

    static abstract class DownloadState {
        DownloadStateContext mContext;

        DownloadState(DownloadStateContext context) {
            mContext = context;
        }

        abstract void displayNowState(TextView status, Button action);

        abstract void handle(TextView status, Button action, Callback callback);

    }

    private static class NormalState extends DownloadState {

        NormalState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            status.setText("");
            action.setText("开始");
        }

        @Override
        void handle(TextView status, Button action, Callback callback) {
            callback.startDownload();
        }
    }

    private static class StartedState extends DownloadState {

        StartedState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            status.setText("正在下载...");
            action.setText("暂停");
        }

        @Override
        void handle(TextView status, Button action, Callback callback) {
            callback.pauseDownload();
        }
    }

    private static class PausedState extends DownloadState {

        PausedState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            status.setText("下载已暂停");
            action.setText("继续");
        }

        @Override
        void handle(TextView status, Button action, Callback callback) {
            callback.startDownload();
        }
    }

    private static class FailedState extends DownloadState {

        FailedState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            status.setText("下载失败");
            action.setText("开始");
        }

        @Override
        void handle(TextView status, Button action, Callback callback) {
            callback.startDownload();
        }
    }

    private static class CanceledState extends DownloadState {

        CanceledState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            status.setText("下载取消");
            action.setText("开始");
        }

        @Override
        void handle(TextView status, Button action, Callback callback) {
            callback.startDownload();
        }
    }

    private static class CompletedState extends DownloadState {

        CompletedState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            status.setText("下载已完成");
            action.setText("安装");
        }

        @Override
        void handle(TextView status, Button action, Callback callback) {
            callback.install();
        }
    }

    private static class InstallState extends DownloadState {

        InstallState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void displayNowState(TextView status, Button action) {
            status.setText("正在安装...");
            action.setText("安装中");
        }

        @Override
        void handle(TextView status, Button action, Callback callback) {
            //doNothing..
        }
    }
}

