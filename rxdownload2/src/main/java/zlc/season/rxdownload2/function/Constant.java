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
            = "Context is NULL! You should call [RxDownload.context(Context context)] first!";

    public static final String DOWNLOAD_URL_EXISTS
            = "The url download task already exists.";

    public static final String DOWNLOAD_RECORD_FILE_DAMAGED
            = "Record file may be damaged, so we will re-download";

    //Normal download hint
    public static final String CHUNKED_DOWNLOAD_HINT = "AHA, CHUNKED DOWNLOAD!";
    public static final String NORMAL_DOWNLOAD_PREPARE = "NORMAL DOWNLOAD PREPARE...";
    public static final String NORMAL_DOWNLOAD_STARTED = "NORMAL DOWNLOAD STARTED...";
    public static final String NORMAL_DOWNLOAD_COMPLETED = "NORMAL DOWNLOAD COMPLETED!";
    public static final String NORMAL_DOWNLOAD_FAILED = "NORMAL DOWNLOAD FAILED OR CANCEL!";

    //Continue download hint
    public static final String CONTINUE_DOWNLOAD_PREPARE = "CONTINUE DOWNLOAD PREPARE...";
    public static final String CONTINUE_DOWNLOAD_STARTED = "CONTINUE DOWNLOAD STARTED...";
    public static final String CONTINUE_DOWNLOAD_COMPLETED = "CONTINUE DOWNLOAD COMPLETED!";
    public static final String CONTINUE_DOWNLOAD_FAILED = "CONTINUE DOWNLOAD FAILED OR CANCEL!";

    //Multi-thread download hint
    public static final String MULTITHREADING_DOWNLOAD_PREPARE
            = "MULTITHREADING DOWNLOAD PREPARE...";
    public static final String MULTITHREADING_DOWNLOAD_STARTED
            = "MULTITHREADING DOWNLOAD STARTED...";
    public static final String MULTITHREADING_DOWNLOAD_COMPLETED
            = "MULTITHREADING DOWNLOAD COMPLETED!";
    public static final String MULTITHREADING_DOWNLOAD_FAILED
            = "MULTITHREADING DOWNLOAD FAILED OR CANCEL!";

    public static final String ALREADY_DOWNLOAD_HINT = "FILE ALREADY DOWNLOADED!";
    public static final String UNABLE_DOWNLOAD_HINT = "UNABLE DOWNLOADED!";
    public static final String NOT_SUPPORT_HEAD_HINT = "NOT SUPPORT HEAD, NOW TRY GET!";

    //Range download hint
    public static final String RANGE_DOWNLOAD_STARTED =
            "[%s] start download! From [%s] to [%s] !";

    public static final String RANGE_DOWNLOAD_COMPLETED =
            "[%s] download completed!";

    public static final String RANGE_DOWNLOAD_CANCELED =
            "[%s] download canceled!";

    public static final String RANGE_DOWNLOAD_FAILED =
            "[%s] download failed or cancel!";

    public static final String RETRY_HINT =
            "[%s] got an [%s] error! [%d] attempt reconnection!";

    //Dir hint
    public static final String DIR_EXISTS_HINT = "Path [%s] exists.";
    public static final String DIR_NOT_EXISTS_HINT =
            "Path [%s] not exists, so create.";
    public static final String DIR_CREATE_SUCCESS = "Path [%s] create success.";
    public static final String DIR_CREATE_FAILED = "Path [%s] create failed.";
    public static final String FILE_DELETE_SUCCESS = "File [%s] delete success.";
    public static final String FILE_DELETE_FAILED = "File [%s] delete failed.";
}
