package zlc.season.rxdownload2.entity;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/17
 * Time: 22:06
 * FIXME
 */
public class DownloadBean {
    private String url;
    private String saveName;
    private String savePath;
    private String extra1;
    private String extra2;
    private String extra3;
    private String extra4;
    private String extra5;

    public DownloadBean() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getExtra1() {
        return extra1;
    }

    public void setExtra1(String extra1) {
        this.extra1 = extra1;
    }

    public String getExtra2() {
        return extra2;
    }

    public void setExtra2(String extra2) {
        this.extra2 = extra2;
    }

    public String getExtra3() {
        return extra3;
    }

    public void setExtra3(String extra3) {
        this.extra3 = extra3;
    }

    public String getExtra4() {
        return extra4;
    }

    public void setExtra4(String extra4) {
        this.extra4 = extra4;
    }

    public String getExtra5() {
        return extra5;
    }

    public void setExtra5(String extra5) {
        this.extra5 = extra5;
    }

    public static class Builder {
        private String url;
        private String saveName;
        private String savePath;
        private String extra1;
        private String extra2;
        private String extra3;
        private String extra4;
        private String extra5;

        public Builder(String url) {
            this.url = url;
        }

        public Builder setSaveName(String saveName) {
            this.saveName = saveName;
            return this;
        }

        public Builder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public Builder setExtra1(String extra1) {
            this.extra1 = extra1;
            return this;
        }

        public Builder setExtra2(String extra2) {
            this.extra2 = extra2;
            return this;
        }

        public Builder setExtra3(String extra3) {
            this.extra3 = extra3;
            return this;
        }

        public Builder setExtra4(String extra4) {
            this.extra4 = extra4;
            return this;
        }

        public Builder setExtra5(String extra5) {
            this.extra5 = extra5;
            return this;
        }

        public DownloadBean build() {
            DownloadBean bean = new DownloadBean();
            bean.url = this.url;
            bean.saveName = this.saveName;
            bean.savePath = this.savePath;
            bean.extra1 = this.extra1;
            bean.extra2 = this.extra2;
            bean.extra3 = this.extra3;
            bean.extra4 = this.extra4;
            bean.extra5 = this.extra5;
            return bean;
        }
    }
}
