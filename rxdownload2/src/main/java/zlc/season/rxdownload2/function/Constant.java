package zlc.season.rxdownload2.function;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/1/13
 * FIXME
 */
public final class Constant {
    public static final String TAG = "RxDownload";
    public static final String TEST_RANGE_SUPPORT = "bytes=0-";
    public static final String CONTEXT_NULL_HINT
            = "Context is NULL! You should call #RxDownload.context(Context context)# first!";

    public static final String DOWNLOAD_URL_EXISTS
            = "This url download task already exists, so do nothing.";

    public static final String DOWNLOAD_RECORD_FILE_DAMAGED
            = "The download record file may be damaged,so we will re download";

    public static final String CHUNKED_DOWNLOAD_HINT = "Chunked download";

    public static final String NORMAL_DOWNLOAD_COMPLETED = "NORMAL DOWNLOAD COMPLETED!";

    public static final String NORMAL_DOWNLOAD_FAILED = "NORMAL DOWNLOAD FAILED OR CANCEL!";

    public static final String RETRY_HINT =
            "The %s thread got an %s error! A %d attempt reconnection!";

    public static final String RANGE_DOWNLOAD_STARTED
            = "The %s thread start download! From %d to %d !";

    public static final String RANGE_DOWNLOAD_COMPLETED =
            "The %s thread download completed! Download size is %s bytes!";

    public static final String RANGE_DOWNLOAD_FAILED =
            "The %s thread download failed or cancel!";

    public static final String DIR_EXISTS_HINT = "The directory of path [%s] exists.";

    public static final String DIR_NOT_EXISTS_HINT =
            "The directory of path [%s] not exists, so create";

    public static final String DIR_CREATE_SUCCESS = "The directory of path [%s] create success.";
    public static final String DIR_CREATE_FAILED = "The directory of path [%s] create failed.";

}
