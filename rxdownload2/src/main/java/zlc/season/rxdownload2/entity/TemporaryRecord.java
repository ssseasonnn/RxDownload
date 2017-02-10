package zlc.season.rxdownload2.entity;

import org.reactivestreams.Publisher;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import retrofit2.Response;
import zlc.season.rxdownload2.function.DownloadApi;
import zlc.season.rxdownload2.function.FileHelper;

import static android.text.TextUtils.concat;
import static java.io.File.separator;
import static zlc.season.rxdownload2.function.Constant.CACHE;
import static zlc.season.rxdownload2.function.Constant.LMF_SUFFIX;
import static zlc.season.rxdownload2.function.Constant.TMP_SUFFIX;
import static zlc.season.rxdownload2.function.Utils.empty;
import static zlc.season.rxdownload2.function.Utils.mkdirs;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/4
 * FIXME
 */
public class TemporaryRecord {
    private String url;
    private String saveName;
    private String savePath;

    private String filePath;
    private String tempPath;
    private String lmfPath;

    private int maxRetryCount;
    private int maxThreads;

    private long contentLength;
    private String lastModify;

    private boolean rangeSupport = false;
    private boolean serverFileChanged = false;

    private FileHelper fileHelper;
    private DownloadApi downloadApi;

    private DownloadType downloadType;

    public TemporaryRecord(String filePath, String tempPath, String lmfPath) {
        this.filePath = filePath;
        this.tempPath = tempPath;
        this.lmfPath = lmfPath;
    }

    /**
     * init needs info
     *
     * @param maxRetryCount   Max retry times
     * @param maxThreads      Max download threads
     * @param defaultSavePath Default save path;
     * @param downloadApi     API
     */
    public void initializeEnvironment(int maxRetryCount, int maxThreads, String defaultSavePath, DownloadApi downloadApi) {
        this.maxThreads = maxThreads;
        this.maxRetryCount = maxRetryCount;
        this.downloadApi = downloadApi;
        this.fileHelper = new FileHelper(maxThreads);

        String realSavePath;
        if (empty(savePath)) {
            realSavePath = defaultSavePath;
        } else {
            realSavePath = savePath;
        }
        String cachePath = concat(realSavePath, separator, CACHE).toString();

        filePath = concat(realSavePath, separator, saveName).toString();
        tempPath = concat(cachePath, separator, saveName, TMP_SUFFIX).toString();
        lmfPath = concat(cachePath, separator, saveName, LMF_SUFFIX).toString();

        mkdirs(realSavePath, cachePath);
    }

    public void prepareNormalDownload() throws IOException, ParseException {
        fileHelper.prepareDownload(getLastModifyFile(), getFile(), contentLength, lastModify);
    }

    public void prepareRangeDownload() throws IOException, ParseException {
        fileHelper.prepareDownload(getLastModifyFile(), getTempFile(), getFile(), contentLength, lastModify);
    }

    public DownloadRange readDownloadRange(int index) throws IOException {
        return fileHelper.readDownloadRange(getTempFile(), index);
    }

    public void save(FlowableEmitter<DownloadStatus> e, Response<ResponseBody> response) {
        fileHelper.saveFile(e, getFile(), response);
    }

    public void save(FlowableEmitter<DownloadStatus> emitter, int index, ResponseBody response) throws IOException {
        DownloadRange range = readDownloadRange(index);
        fileHelper.saveFile(emitter, index, range.start, range.end, getTempFile(), getFile(), response);
    }

    public Flowable<Response<ResponseBody>> download() {
        return downloadApi.download(null, url);
    }

    public Flowable<Response<ResponseBody>> rangeDownload(final int index) {
        return Flowable.create(new FlowableOnSubscribe<DownloadRange>() {
            @Override
            public void subscribe(FlowableEmitter<DownloadRange> e) throws Exception {
                DownloadRange range = readDownloadRange(index);
                if (range.legal()) {
                    e.onNext(range);
                }
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR).flatMap(new Function<DownloadRange, Publisher<Response<ResponseBody>>>() {
            @Override
            public Publisher<Response<ResponseBody>> apply(DownloadRange range) throws Exception {
                String rangeStr = "bytes=" + range.start + "-" + range.end;
                return downloadApi.download(rangeStr, url);
            }
        });
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public boolean isSupportRange() {
        return rangeSupport;
    }

    public void setRangeSupport(boolean rangeSupport) {
        this.rangeSupport = rangeSupport;
    }

    public DownloadType getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(DownloadType downloadType) {
        this.downloadType = downloadType;
    }

    public boolean isServerFileChanged() {
        return serverFileChanged;
    }

    public void setServerFileChanged(boolean serverFileChanged) {
        this.serverFileChanged = serverFileChanged;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setLastModify(String lastModify) {
        this.lastModify = lastModify;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public File getFile() {
        return new File(filePath);
    }

    public File getTempFile() {
        return new File(tempPath);
    }

    public File getLastModifyFile() {
        return new File(lmfPath);
    }

    public boolean fileExists() {
        return getFile().exists();
    }

    public boolean tempExists() {
        return getTempFile().exists();
    }

    public boolean fileComplete() {
        return getFile().length() == contentLength;
    }

    public boolean tempFileDamaged() throws IOException {
        return fileHelper.tempFileDamaged(getTempFile(), contentLength);
    }

    public String readLastModify() throws IOException {
        return fileHelper.getLastModify(getLastModifyFile());
    }

    public boolean fileNotComplete() throws IOException {
        return fileHelper.fileNotComplete(getTempFile());
    }
}
