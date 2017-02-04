package zlc.season.rxdownload2.entity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/4
 * FIXME
 */
public class TemporaryRecordTable {
    private Map<String, TemporaryRecord> map;

    public TemporaryRecordTable() {
        this.map = new HashMap<>();
    }

    public void addDownloadRecord(String url, String saveName, String savePath)
            throws IOException {

//        map.put(url, getRealFilePaths(saveName, savePath));
    }

    public void add(String url, TemporaryRecord record) {

    }

    public void update(String url, TemporaryRecord record) {

    }

    public void update(String url, String saveName) {
        map.get(url).setSaveName(saveName);
    }

    public void update(String url, boolean range) {
        if (range) {
            map.get(url).supportRange();
        } else {
            map.get(url).notSupportRange();
        }
    }

    public TemporaryRecord get(String url) {
        return map.get(url);
    }

    public boolean exists(String url) {
        return map.get(url) != null;
    }

    public void delete(String url) {
        map.remove(url);
    }

    public File getFile(String url) {
        return new File(map.get(url).getFilePath());
    }

    public File getTempFile(String url) {
        return new File(map.get(url).getTempPath());
    }

    public File getLastModifyFile(String url) {
        return new File(map.get(url).getLmfPath());
    }
}
