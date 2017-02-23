package zlc.season.rxdownload2;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import zlc.season.rxdownload2.function.Utils;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/1
 * Time: 17:05
 * FIXME
 */
public class UtilsTest {
    @Test
    public void GMTToLong() throws Exception {
        long l = Utils.GMTToLong("Fri, 04 Oct 2016 00:35:29 GMT");
        System.out.println(l);
        long a = Utils.GMTToLong(null);
        System.out.println(a);
    }

    @Test
    public void longToGMT() throws Exception {
        String str = Utils.longToGMT(1475541329000L);
        System.out.println(str);
    }

    @Test
    public void substringTest() throws Exception {
        String str = "http://dldir1.qq.com/weixin/android/weixin6330android920.apk";
        System.out.println(str.substring(str.lastIndexOf('/') + 1));
    }

    @Test
    public void contentDisposition() throws Exception {
        String test = "attachment; filename=\"com.coolapk.market_7.3.2_1701250.apk\"";
        Matcher m = Pattern.compile(".*filename=(.*)").matcher(test.toLowerCase());
        String result;
        if (m.find()) {
            result = m.group(1);
        } else {
            result = "";
        }
        System.out.println(result);

        if (result.startsWith("\"")) {
            result = result.substring(1);
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length()-1);
        }
        System.out.println(result);
    }
}