package zlc.season.rxdownload2.function;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;

import org.reactivestreams.Publisher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiPredicate;
import okhttp3.internal.http.HttpHeaders;
import retrofit2.Response;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Locale.getDefault;
import static java.util.TimeZone.getTimeZone;
import static zlc.season.rxdownload2.function.Constant.DIR_CREATE_FAILED;
import static zlc.season.rxdownload2.function.Constant.DIR_CREATE_SUCCESS;
import static zlc.season.rxdownload2.function.Constant.DIR_EXISTS_HINT;
import static zlc.season.rxdownload2.function.Constant.DIR_NOT_EXISTS_HINT;
import static zlc.season.rxdownload2.function.Constant.FILE_DELETE_FAILED;
import static zlc.season.rxdownload2.function.Constant.FILE_DELETE_SUCCESS;
import static zlc.season.rxdownload2.function.Constant.RETRY_HINT;
import static zlc.season.rxdownload2.function.Constant.TAG;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/2
 * Time: 09:07
 * 工具类
 */
public class Utils {

    public static void log(String message) {
        Log.i(TAG, message);
    }

    public static void log(String message, Object... args) {
        log(format(getDefault(), message, args));
    }

    public static void log(Throwable throwable) {
        Log.w(TAG, throwable);
    }

    public static boolean notEmpty(String string) {
        return !TextUtils.isEmpty(string);
    }

    public static boolean empty(String string) {
        return TextUtils.isEmpty(string);
    }

    /**
     * convert long to GMT string
     *
     * @param lastModify long
     *
     * @return String
     */
    public static String longToGMT(long lastModify) {
        Date d = new Date(lastModify);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(getTimeZone("GMT"));
        return sdf.format(d);
    }

    /**
     * convert GMT string to long
     *
     * @param GMT String
     *
     * @return long
     *
     * @throws ParseException
     */
    public static long GMTToLong(String GMT) throws ParseException {
        if (GMT == null || "".equals(GMT)) {
            return new Date().getTime();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(getTimeZone("GMT"));
        Date date = sdf.parse(GMT);
        return date.getTime();
    }

    public static void close(Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public static <U> ObservableTransformer<U, U> retry(final int MAX_RETRY_COUNT) {
        return new ObservableTransformer<U, U>() {
            @Override
            public ObservableSource<U> apply(Observable<U> upstream) {
                return upstream.retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(Integer integer, Throwable throwable) throws Exception {
                        return retry(MAX_RETRY_COUNT, integer, throwable);
                    }
                });
            }
        };
    }

    public static <U> FlowableTransformer<U, U> retry2(final int MAX_RETRY_COUNT) {
        return new FlowableTransformer<U, U>() {
            @Override
            public Publisher<U> apply(Flowable<U> upstream) {
                return upstream.retry(new BiPredicate<Integer, Throwable>() {
                    @Override
                    public boolean test(Integer integer, Throwable throwable) throws Exception {
                        return retry(MAX_RETRY_COUNT, integer, throwable);
                    }
                });
            }
        };
    }

    public static void dispose(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public static String lastModify(Response<?> response) {
        return response.headers().get("Last-Modified");
    }

    public static long contentLength(Response<?> response) {
        return HttpHeaders.contentLength(response.headers());
    }

    public static boolean isChunked(Response<?> response) {
        return "chunked".equals(transferEncoding(response));
    }

    public static boolean notSupportRange(Response<?> response) {
        return TextUtils.isEmpty(contentRange(response)) || contentLength(response) == -1 ||
                isChunked(response);
    }

    public static boolean serverFileChanged(Response<Void> resp) {
        return resp.code() == 200;
    }

    public static boolean serverFileNotChange(Response<Void> resp) {
        return resp.code() == 206;
    }

    public static boolean requestRangeNotSatisfiable(Response<Void> resp) {
        return resp.code() == 416;
    }

    public static void installApk(Context context, File file) {
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /**
     * Format file size to String
     *
     * @param size long
     *
     * @return String
     */
    public static String formatSize(long size) {
        String hrSize;
        double b = size;
        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);
        DecimalFormat dec = new DecimalFormat("0.00");
        if (t > 1) {
            hrSize = dec.format(t).concat(" TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(b).concat(" B");
        }
        return hrSize;
    }

    public static Boolean retry(int MAX_RETRY_COUNT, Integer integer, Throwable throwable) {
        if (throwable instanceof ProtocolException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                log(RETRY_HINT, currentThread().getName(), "ProtocolException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof UnknownHostException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                log(RETRY_HINT, currentThread().getName(), "UnknownHostException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof HttpException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                log(RETRY_HINT, currentThread().getName(), "HttpException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof SocketTimeoutException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                log(RETRY_HINT, currentThread().getName(), "SocketTimeoutException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof ConnectException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                log(RETRY_HINT, currentThread().getName(), "ConnectException", integer);
                return true;
            }
            return false;
        } else if (throwable instanceof SocketException) {
            if (integer < MAX_RETRY_COUNT + 1) {
                log(RETRY_HINT, currentThread().getName(), "SocketException", integer);
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * create dirs with params path
     *
     * @param paths paths
     *
     * @throws IOException
     */
    public static void mkdirs(String... paths) throws IOException {
        for (String each : paths) {
            File file = new File(each);
            if (file.exists() && file.isDirectory()) {
                log(DIR_EXISTS_HINT, each);
            } else {
                log(DIR_NOT_EXISTS_HINT, each);
                boolean flag = file.mkdir();
                if (flag) {
                    log(DIR_CREATE_SUCCESS, each);
                } else {
                    log(DIR_CREATE_FAILED, each);
                    throw new IOException(format(getDefault(), DIR_CREATE_FAILED, each));
                }
            }
        }
    }

    /**
     * delete files
     *
     * @param files files
     */
    public static void deleteFile(File... files) {
        for (File each : files) {
            if (each.exists()) {
                boolean flag = each.delete();
                if (flag) {
                    log(format(getDefault(), FILE_DELETE_SUCCESS, each.getName()));
                } else {
                    log(format(getDefault(), FILE_DELETE_FAILED, each.getName()));
                }
            }
        }
    }

    private static String transferEncoding(Response<?> response) {
        return response.headers().get("Transfer-Encoding");
    }

    private static String contentRange(Response<?> response) {
        return response.headers().get("Content-Range");
    }
}
