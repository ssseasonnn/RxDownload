package zlc.season.rxdownload;

import java.io.File;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/1
 * Time: 10:38
 * FIXME
 */
public class EtagFile extends File {
    private String mEtag;

    public EtagFile(String pathname) {
        super(pathname);
    }

    public String getEtag() {
        return mEtag;
    }

    public void setEtag(String etag) {
        mEtag = etag;
    }
}
