package zlc.season.rxdownload2;

import android.support.annotation.NonNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Asura on 2017/4/1.
 */

public class ExecutorHelper {
    public static final ThreadPoolExecutor LOW;
    public static final ThreadPoolExecutor BALANCE;
    public static final ThreadPoolExecutor HIGH;

    static {
        LOW = new ThreadPoolExecutor(3, 3, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PayLoadThreadFactory("low"));
        LOW.allowCoreThreadTimeOut(true);
        int countOfCPU = Runtime.getRuntime().availableProcessors();
        BALANCE = new ThreadPoolExecutor(countOfCPU, countOfCPU * 2 + 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new PayLoadThreadFactory("balance"));
        HIGH = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new PayLoadThreadFactory("high"));
    }

    private static class PayLoadThreadFactory implements ThreadFactory {
        private AtomicInteger incrementAndGet = new AtomicInteger();
        private final String poolName;

        public PayLoadThreadFactory(String poolName) {
            this.poolName = poolName;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, poolName + "-pool-thread-" + incrementAndGet.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
