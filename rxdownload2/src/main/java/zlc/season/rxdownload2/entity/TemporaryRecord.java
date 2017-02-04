package zlc.season.rxdownload2.entity;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/4
 * FIXME
 */
public class TemporaryRecord {
    public static final int EMPTY = 0;
    public static final int SUPPORT_RANGE = 1;
    public static final int NOT_SUPPORT_RANGE = 2;

    private String url;
    private String saveName;
    private String savePath;

    private String filePath;
    private String tempPath;
    private String lmfPath;

    private int rangeSupportFlag = EMPTY;

    public TemporaryRecord(String filePath, String tempPath, String lmfPath) {
        this.filePath = filePath;
        this.tempPath = tempPath;
        this.lmfPath = lmfPath;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getRangeSupportFlag() {
        return rangeSupportFlag;
    }

    public boolean isEmptyFlag() {
        return rangeSupportFlag == EMPTY;
    }

    public boolean isSupportRange() {
        return rangeSupportFlag == SUPPORT_RANGE;
    }

    public void supportRange() {
        this.rangeSupportFlag = SUPPORT_RANGE;
    }

    public void notSupportRange() {
        this.rangeSupportFlag = NOT_SUPPORT_RANGE;
    }

    public String getTempPath() {
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public String getLmfPath() {
        return lmfPath;
    }

    public void setLmfPath(String lmfPath) {
        this.lmfPath = lmfPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
