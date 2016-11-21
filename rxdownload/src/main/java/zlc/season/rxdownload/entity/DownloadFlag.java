package zlc.season.rxdownload.entity;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/17
 * Time: 11:53
 * FIXME
 */
public class DownloadFlag {
    public static final int NORMAL = 9990;   //下载未开始
    public static final int STARTED = 9991;  //下载已开始
    public static final int PAUSED = 9992;   //下载已暂停
    public static final int CANCELED = 9993; //下载已取消
    public static final int COMPLETED = 9994;//下载已完成
    public static final int FAILED = 9995;   //下载已失败
    public static final int WAITING = 9996;   //等待下载

    //这2个字段留给用户自行使用
    public static final int INSTALL = 9997;
    public static final int INSTALLED = 9998;
}
