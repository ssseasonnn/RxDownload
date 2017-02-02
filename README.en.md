# RxDownload
The download tool based on RxJava . Support multi-threaded download and breakpoint download, intelligent judge whether to support multi-threaded download and breakpoint download.


RxJava based download tool to support multi-threaded download and HTTP, self
deciding whether to support HTTP or other functions

Tags (space separated): Android RxJava Download Tools Multi-threaded

*Read this in other languages: [中文](README.md), [English](README.en.md)*

---

### Update Log:：

[Update log moved here](https://github.com/ssseasonnn/RxDownload/blob/master/CHANGE_LOG.md)



### Main Functions:

- Use Retrofit + OKHTTP to make network requests
- RxJava based, support RxJava chain of various operators
- Use multi-threaded breakpoint download server supports HTTP
- Use traditional download when server doesn't support HTTP
- Multi-threaded download, maximum thread setting available , with default value of 3
- If the network connection fails to reconnect automatically, the maximum retry times can be configured with default value of times
- MappedByteBuffer memory mapping in Java NIO for efficient reading and writing files
- Streaming download, without causi g memory leak due to large files
- Last-Modified field automatically determine the server-side file changes
- Reduces the amount of communication data by using a lightweight HEAD request method to obtain only response headers during authentication with the server



### Effect diagram


<img title="普通下载" width="30%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/basic_download.gif">
<img title="Service下载" width="30%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/service_download.gif">
<img title="下载管理"  width="33%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/download_manager.gif">


### Download the flow chart

<img src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/download.png" title="下载流程图">



# Notes, please read carefully:

Download links are random from the Internet in demo, therefore with frequent address errors or download failures and etc. If you download the Demo and the results are wrong, please first replace the download link for testing.

### How to Use

#### 1. Preparation

1.Add Gradle dependencies

 [ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload/_latestVersion)

```groovy
	dependencies{
   		 compile 'zlc.season:rxdownload:1.2.8'  //不再继续维护，最终版本
	}
```

**Pleaer note that there is no support for this version, but you can continue to use the previous version. It is recommended to migrate to RxJava2 as soon as possible**

2.For RxJava2

RxDownload now supports RxJava2, simply change the package name to ```zlc.season.rxdownload2.```

[ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload2/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload2/_latestVersion)

```groovy
	dependencies{
   		 compile 'zlc.season:rxdownload2:1.1.1'
	}
```

3.Configure the permissions

```xml
 	<!-- 在XML中设置权限 -->
	<uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

> **Please note that Android 6.0 and above must also apply run-time permissions, if you are unable to download, check permissions**

#### 2. Regular Download

- Background download unavailable
- Unsubscribing pauses the download

1.How to use

```java
		    RxDownload.getInstance()
                  .download(url, "weixin.apk", null)
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(new Observer<DownloadStatus>() {
                      @Override
                      public void onSubscribe(Disposable d) {
                          mDisposable = d;
                      }

                      @Override
                      public void onNext(DownloadStatus value) {
						//获得下载状态
                      }

                      @Override
                      public void onError(Throwable e) {
						//下载出错
                      }

                      @Override
                      public void onComplete() {
						//下载完成
                      }
                  });
```

> Parameters: download addresses, saved file names, saved addresses.
>
> Url and saveName as a must-pass parameter, savePath is optional, the default download address for / storage / emulated / 0 / Download / directory, which is built-in storage Download directory

2.Parameter configuration

**The parameters that can be configured are as following:**

```java
	 RxDownload.getInstance()
                .maxThread(10)                    //设置最大线程
                .maxRetryCount(10)                //设置下载失败重试次数
                .retrofit(myRetrofit)             //若需要自己的retrofit客户端,可在这里指定
                .defaultSavePath(defaultSavePath) //设置默认的下载路径
                .context(this)                    //自动安装需要Context
                .autoInstall(true);               //下载完成自动安装，仅限7.0以下，7.0以上自行提供FileProvider
                .download(url,savename,savepath)  //开始下载
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DownloadStatus>() {);
```

**Tips: **

- RxDownload.getInstance () returns a completely new object each time.
- Each instance can be individually set the maximum thread, the default path and other parameters.
- Therefore, you should avoid creating instances more than once when you create multiple download tasks.

```java
RxDownload rxDownload1 = RxDownload.getInstance()
  					   .maxThread(5)
  					   .maxRetryCount(10)
  					   .defaultSavePath(defaultPath);
//download task 1:
Disposable d1 = rxDownload1.download(url1,name1,null)...
//download task 2:
Disposable d2 = rxDownload1.download(url2,name2,null)...

RxDownload rxDownload2 = RxDownload.getInstance()
  					   .maxThread(10)...
//download task 3:
Disposable d3 = rxDownload2.download(url3,name3,null)...
```

3.Cancel or pause download

**Unsubscribe to pause the download**

```java
Disposable disposable = RxDownload.getInstance()
                .download(url, null, null)
  				//...

//取消订阅, 即可暂停下载, 若服务端不支持断点续传,下一次下载会重新下载,反之会继续下载
if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
}
```

4.Provides a transferform method for RxJava Compose operator use

eg. in conjunction with RxPermission

> RxPermission is a library for resolving run-time permissions for Android 6.0
  address of the library: RxPermission: [RxPermission](https://github.com/tbruyelle/RxPermissions)

```java
 disposable =  RxPermissions.getInstance(mContext)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE) //申请存储卡权限
                    .doOnNext(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean granted)  throws Exception {
                            if (!granted) {  //权限被拒绝
                                throw new RuntimeException("no permission");
                            }
                        }
                    })
                    .observeOn(Schedulers.io())
                    .compose(RxDownload.getInstance().<Boolean>transform(data.url, data.name, null))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<DownloadStatus>() { ... });
```

#### 3. Service download

- Use Service to download, with background download capabilities
- Canceling a subscription does not cause the download to pause
- Be able to download progress in real time
- while saving download records to the database
- The ability to set the maximum number of downloads, when adding tasks to the download queue, the extra download task will wait until command to automatically restart the download.

1.Start download and add it to the download queue.

```java
  RxDownload.getInstance()
                .context(this)
                .autoInstall(true); //下载完成自动安装
                .maxDownloadNumber(3)  //设置同时最大下载数量
                .serviceDownload(url, saveName, defaultPath)
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception{
                        Toast.makeText(ServiceDownloadActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
                    }
                });
```

**ServiceDownload () no longer uses the broadcast method, and no longer receives the download progress, so no asynchronous operation, no need to unsubscribe.**

2.Receive the download events and status. Cancel the subscription to cancel the reception.

```java
Disposable disposable =  mRxDownload.receiveDownloadStatus(url)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent event) throws Exception {
                        //当事件为Failed时, 才会有异常信息, 其余时候为null.
                        if (event.getFlag() == DownloadFlag.FAILED) {
                            Throwable throwable = event.getError();
                            Log.w("Error", throwable);
                        }
                        mDownloadController.setEvent(event);
                        updateProgress(event);
                    }
                });
// 取消订阅即可取消接收
```

**TIPS:**

- Regardless of whether the task starts to download, acquire the url of the corresponding events and status is available
- No longer need to read the download records from the database alone.
- Please note that now accepting the download progress will no longer receive the onError and onComplete events, which are placed in the DownloadEvent.

3.Download the DownloadEvent description

```java
public class DownloadEvent {
    private int flag = DownloadFlag.NORMAL;  //当前下载的状态
    private DownloadStatus downloadStatus = new DownloadStatus();  //下载进度

    public int getFlag() {
        return flag;
    }
    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }
}
```

A flag is added to the DownloadEvent to mark the current download task in any of the following status:

```java
public class DownloadFlag {
    public static final int NORMAL = 9990;      //未下载
    public static final int WAITING = 9991;     //等待中
    public static final int STARTED = 9992;     //已开始下载
    public static final int PAUSED = 9993;      //已暂停
    public static final int CANCELED = 9994;    //已取消
    public static final int COMPLETED = 9995;   //已完成
    public static final int FAILED = 9996;      //下载失败
    public static final int INSTALL = 9997;     //安装中,暂未使用
    public static final int INSTALLED = 9998;   //已安装,暂未使用
    public static final int DELETED = 9999;     //已删除
}
```

When the event is received in the onNext (DownloadEvent event), the state of the flag can be used to determine the status of the current download task, and thus to perform different operations.

4.Suspend the download: suspend the url download as download task

```java
 mRxDownload.pauseServiceDownload(url).subscribe();
```

5.Cancel the download: cancel url download as the download task

```java
 mRxDownload.cancelServiceDownload(url).subscribe();
```

6.Delete the download: delete the download, and delete the task from the database

**Optional to delete the downloaded file**

```java
mRxDownload.deleteServiceDownload(url,true or false).subscribe();
```

7.Acquire  all the download records: access to all the download records in the database

```java
mRxDownload.getTotalDownloadRecords()
                .subscribe(new Action1<List<DownloadRecord>>() {
                    @Override
                    public void call(List<DownloadRecord> list) {
                        mAdapter.addAll(list);
                    }
                });
```

8.Acquire downloaded file

```java
File file = mRxDownload.getRealFiles(saveName, defaultPath)[0];
```

9.More follow-up features will be gradually improved

If you have questions, please feel free to ask

### About Me

If you want to communicate on this project, you can contact me via the following:

QQ : 270362455

QQ群：603610731

Gmail: ssseasonnn@gmail.com

### License

> ```
> Copyright 2016 Season.Zlc
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
> ```