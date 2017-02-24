package zlc.season.rxdownload2.function;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/1/13
 * FIXME
 */
public final class Constant {
    public static final String TAG = "RxDownload";

    public static final String TMP_SUFFIX = ".tmp";  //temp file
    public static final String LMF_SUFFIX = ".lmf";  //last modify file
    public static final String CACHE = ".cache";    //cache directory

    /**
     * test
     */
    public static final String TEST_RANGE_SUPPORT = "bytes=0-";


    public static final String URL_ILLEGAL
            = "The url [%s] is illegal.";

    public static final String DOWNLOAD_URL_EXISTS
            = "The url [%s] already exists.";

    public static final String DOWNLOAD_RECORD_FILE_DAMAGED
            = "Record file may be damaged, so we will re-download";

    //Normal download hint
    public static final String CHUNKED_DOWNLOAD_HINT = "Aha, chunked download!";
    public static final String NORMAL_DOWNLOAD_PREPARE = "Normal download prepare...";
    public static final String NORMAL_DOWNLOAD_STARTED = "Normal download started...";
    public static final String NORMAL_DOWNLOAD_COMPLETED = "Normal download completed!";
    public static final String NORMAL_DOWNLOAD_FAILED = "Normal download failed!";
    public static final String NORMAL_DOWNLOAD_CANCEL = "Normal download cancel!";
    public static final String NORMAL_DOWNLOAD_FINISH = "Normal download finish!";

    //Continue download hint
    public static final String CONTINUE_DOWNLOAD_PREPARE = "Continue download prepare...";
    public static final String CONTINUE_DOWNLOAD_STARTED = "Continue download started...";
    public static final String CONTINUE_DOWNLOAD_COMPLETED = "Continue download completed!";
    public static final String CONTINUE_DOWNLOAD_FAILED = "Continue download failed!";
    public static final String CONTINUE_DOWNLOAD_CANCEL = "Continue download cancel!";
    public static final String CONTINUE_DOWNLOAD_FINISH = "Continue download finish!";

    //Multi-thread download hint
    public static final String MULTITHREADING_DOWNLOAD_PREPARE
            = "Multithreading download prepare...";
    public static final String MULTITHREADING_DOWNLOAD_STARTED
            = "Multithreading download started...";
    public static final String MULTITHREADING_DOWNLOAD_COMPLETED
            = "Multithreading download completed!";
    public static final String MULTITHREADING_DOWNLOAD_FAILED
            = "Multithreading download failed!";
    public static final String MULTITHREADING_DOWNLOAD_CANCEL
            = "Multithreading download cancel!";
    public static final String MULTITHREADING_DOWNLOAD_FINISH
            = "Multithreading download finish!";

    public static final String ALREADY_DOWNLOAD_HINT = "File already downloaded!";

    //Range download hint
    public static final String RANGE_DOWNLOAD_STARTED =
            "Range %d start download from [%d] to [%d]";

    public static final String RANGE_DOWNLOAD_COMPLETED =
            "[%s] download completed!";

    public static final String RANGE_DOWNLOAD_CANCELED =
            "[%s] download canceled!";

    public static final String RANGE_DOWNLOAD_FAILED =
            "[%s] download failed or cancel!";

    public static final String REQUEST_RETRY_HINT = "Request";
    public static final String NORMAL_RETRY_HINT = "Normal download";
    public static final String RANGE_RETRY_HINT = "Range %d";
    public static final String RETRY_HINT =
            "%s get [%s] error, now retry [%d] times";

    //Dir hint
    public static final String DIR_EXISTS_HINT = "Path [%s] exists.";
    public static final String DIR_NOT_EXISTS_HINT =
            "Path [%s] not exists, so create.";
    public static final String DIR_CREATE_SUCCESS = "Path [%s] create success.";
    public static final String DIR_CREATE_FAILED = "Path [%s] create failed.";
    public static final String FILE_DELETE_SUCCESS = "File [%s] delete success.";
    public static final String FILE_DELETE_FAILED = "File [%s] delete failed.";

    public static final String TRY_TO_ACQUIRE_SEMAPHORE = "Try to acquire semaphore...";
    public static final String ACQUIRE_SUCCESS = "Acquire success!";
    public static final String ACQUIRE_SURPLUS_SEMAPHORE = "After acquired, surplus %d semaphore";
    public static final String RELEASE_SURPLUS_SEMAPHORE = "After release, surplus %d semaphore";

    public static final String WAITING_FOR_MISSION_COME = "DownloadQueue waiting for mission come...";
    public static final String MISSION_COMING = "Mission coming!";
}
