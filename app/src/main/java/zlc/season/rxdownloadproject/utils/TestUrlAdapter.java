package zlc.season.rxdownloadproject.utils;

import zlc.season.rxdownload2.ext.IUrlAdapter;

public class TestUrlAdapter implements IUrlAdapter {
    /**
     * @param url the url with token
     * @return the url has been converted
     */
    @Override
    public String convertUrl(String url) {
        //TODO add something to remove the token
        return url;
    }
}
