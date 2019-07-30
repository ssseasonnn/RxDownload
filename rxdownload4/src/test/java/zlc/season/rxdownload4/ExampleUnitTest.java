package zlc.season.rxdownload4;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test_mime_type() {
        String url = "http://www.baidu.com/user/test.htmlsdf?!(@*&#!@#)!@)#qsdfasdf!!!!!!query=123&a=x#";
        String fileExtensionFromUrl = getFileExtensionFromUrl(url);
        System.out.println(fileExtensionFromUrl);
    }

    public static String getFileExtensionFromUrl(String url) {
        if (!url.isEmpty()) {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) {
                url = url.substring(0, fragment);
            }

            int query = url.lastIndexOf('?');
            if (query > 0) {
                url = url.substring(0, query);
            }

            int filenamePos = url.lastIndexOf('/');
            String filename =
                    0 <= filenamePos ? url.substring(filenamePos + 1) : url;

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty() &&
                    Pattern.matches("[a-zA-Z_0-9.\\-()%]+", filename)) {
//                int dotPos = filename.lastIndexOf('.');
//                if (0 <= dotPos) {
//                    return filename.substring(dotPos + 1);
//                }
                return filename;
            }
        }

        return "";
    }
}