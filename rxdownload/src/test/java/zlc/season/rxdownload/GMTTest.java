package zlc.season.rxdownload;

import org.junit.Test;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/1
 * Time: 17:05
 * FIXME
 */
public class GMTTest {
    @Test
    public void lastModifyToGMTStr() throws Exception {
        RxDownload rxDownload = RxDownload.getInstance();
        long l = rxDownload.GMTStrToLastModify("Fri, 28 Oct 2016 00:35:29 GMT");
        System.out.print(l);
    }

    @Test
    public void GMTStrToLastModify() throws Exception {
        RxDownload rxDownload = RxDownload.getInstance();
        String str = rxDownload.lastModifyToGMTStr(1477614929000L);
        System.out.print(str);
    }

}