package zlc.season.rxdownload2.ext;

public class DownloadUrlAdapterFactory {
    private volatile static DownloadUrlAdapterFactory singleton;

    private IUrlAdapter mAdapter;

    public static DownloadUrlAdapterFactory instance() {
        if (singleton == null) {
            synchronized (DownloadUrlAdapterFactory.class) {
                if (singleton == null) {
                    singleton = new DownloadUrlAdapterFactory();
                }
            }
        }
        return singleton;
    }

    public void setAdapter(IUrlAdapter adapter) {
        mAdapter = adapter;
    }

    public String parseUrl(String url) {
        if (mAdapter == null) {
            mAdapter = new DefaultUrlAdapter();
        }
        return mAdapter.convertUrl(url);
    }
}
