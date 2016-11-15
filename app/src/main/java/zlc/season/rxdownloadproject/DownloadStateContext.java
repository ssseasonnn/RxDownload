package zlc.season.rxdownloadproject;

import android.widget.Button;
import android.widget.TextView;

import zlc.season.rxdownload.DownloadRecord;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/15
 * Time: 15:26
 * FIXME
 */
public class DownloadStateContext {
    private DownloadState state;

    private TextView mStatusText;
    private Button mActionButton;

    public DownloadStateContext(TextView statusText, Button actionButton) {
        mStatusText = statusText;
        mActionButton = actionButton;
    }

    public void setState(DownloadState state) {
        this.state = state;
    }

    public void setState(int flag) {
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
        }
    }

    public void displayNowState() {
        state.nowState(mStatusText, mActionButton);
    }

    public void nextState(Callback callback) {
        state.nextState(mStatusText, mActionButton, callback);
    }

    public interface Callback {

        void startDownload();

        void cancelDownload();

        void pauseDownload();
    }

    static abstract class DownloadState {
        DownloadStateContext mContext;

        DownloadState(DownloadStateContext context) {
            mContext = context;
        }

        abstract void nowState(TextView status, Button action);

        abstract void nextState(TextView status, Button action, Callback callback);

    }

    private static class NormalState extends DownloadState {

        NormalState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void nowState(TextView status, Button action) {
            status.setText("");
            action.setText("开始");
        }

        @Override
        void nextState(TextView status, Button action, Callback callback) {
            status.setText("正在下载...");
            action.setText("暂停");
            mContext.setState(new StartedState(mContext));
            callback.startDownload();
        }
    }

    private static class StartedState extends DownloadState {

        StartedState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void nowState(TextView status, Button action) {
            status.setText("正在下载...");
            action.setText("暂停");
        }

        @Override
        void nextState(TextView status, Button action, Callback callback) {
            status.setText("下载已暂停...");
            action.setText("继续");
            mContext.setState(new PausedState(mContext));
            callback.pauseDownload();
        }
    }

    private static class PausedState extends DownloadState {

        PausedState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void nowState(TextView status, Button action) {
            status.setText("下载已暂停");
            action.setText("继续");
        }

        @Override
        void nextState(TextView status, Button action, Callback callback) {
            status.setText("正在下载...");
            action.setText("暂停");
            mContext.setState(new StartedState(mContext));
            callback.startDownload();
        }
    }

    private static class FailedState extends DownloadState {

        FailedState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void nowState(TextView status, Button action) {
            status.setText("下载失败");
            action.setText("开始");
        }

        @Override
        void nextState(TextView status, Button action, Callback callback) {
            status.setText("开始下载");
            action.setText("暂停");
            mContext.setState(new StartedState(mContext));
        }
    }

    private static class CanceledState extends DownloadState {

        CanceledState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void nowState(TextView status, Button action) {
            status.setText("下载取消");
            action.setText("开始");
        }

        @Override
        void nextState(TextView status, Button action, Callback callback) {
            status.setText("开始下载");
            action.setText("暂停");
            mContext.setState(new StartedState(mContext));
        }
    }

    private static class CompletedState extends DownloadState {

        CompletedState(DownloadStateContext context) {
            super(context);
        }

        @Override
        void nowState(TextView status, Button action) {
            status.setText("下载已完成");
            action.setText("安装");
        }

        @Override
        void nextState(TextView status, Button action, Callback callback) {
            status.setText("正在安装");
            action.setText("...");
            //do nothing...
        }
    }
}

