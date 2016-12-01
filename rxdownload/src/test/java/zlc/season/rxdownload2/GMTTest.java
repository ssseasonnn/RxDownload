package zlc.season.rxdownload2;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import zlc.season.rxdownload2.function.Utils;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/1
 * Time: 17:05
 * FIXME
 */
public class GMTTest {
    @Test
    public void GMTToLong() throws Exception {
        long l = Utils.GMTToLong("Fri, 28 Oct 2016 00:35:29 GMT");
        System.out.println(l);
        long a = Utils.GMTToLong(null);
        System.out.println(a);
    }

    @Test
    public void longToGMT() throws Exception {
        String str = Utils.longToGMT(0L);
        System.out.println(str);
    }

    @Test
    public void substringTest() throws Exception {
        String str = "http://dldir1.qq.com/weixin/android/weixin6330android920.apk";
        System.out.println(str.substring(str.lastIndexOf('/') + 1));
    }

    @Test
    public void subscriptionTest() throws Exception {
        Subscription subscription = Observable.just(1)
                .subscribeOn(Schedulers.io())
                .delay(5, SECONDS)
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        throw new RuntimeException("Oops!");
                    }
                })
                .observeOn(Schedulers.test())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("complete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("error");
                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("integer:" + integer);
                    }
                });
        boolean flag = subscription.isUnsubscribed();
        System.out.println("flag:" + flag);
        Thread.sleep(10000);
        System.out.println("flag:" + flag);
    }
}