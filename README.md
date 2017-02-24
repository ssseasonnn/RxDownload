# RxDownload

基于RxJava打造的下载工具, 支持多线程下载和断点续传, 智能判断是否支持断点续传等功能

---

### 更新日志：

[更新日志搬到这里了](https://github.com/ssseasonnn/RxDownload/blob/master/CHANGE_LOG.md)

### 主要功能:

- 使用Retrofit+OKHTTP来进行网络请求
- 基于RxJava打造, 支持RxJava各种操作符链式调用
- 若服务器支持断点续传, 则使用多线程断点下载
- 若不支持断点续传,则进行传统下载
- 多线程下载, 可以设置最大线程, 默认值为3线程
- 网络连接失败自动重连, 可配置最大重试次数, 默认值为3次
- 利用Java NIO 中的 MappedByteBuffer内存映射进行高效读写文件
- 流式下载，再大的文件也不会造成内存泄漏
- 根据Last-Modified字段自动判断服务端文件是否变化
- 与服务器进行验证过程中,使用更轻便的HEAD请求方式仅获取响应头,减轻通信数据量



### 效果图


<img title="普通下载" width="30%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/basic_download.gif">
<img title="Service下载" width="30%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/service_download.gif">
<img title="下载管理"  width="33%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/download_manager.gif">



### 使用方式

#### 一、准备工作

1.添加Gradle依赖

 [ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload/_latestVersion)

```groovy
	dependencies{
   		 compile 'zlc.season:rxdownload:1.2.8'  //不再继续维护，最终版本
	}
```

**注意：该版本已经不再继续维护了，但是你可以继续使用之前的版本，建议尽快迁移至RxJava2**

2.For RxJava2 

RxDownload 现在支持RxJava2, 只需将包名改为 ```zlc.season.rxdownload2.``` 

[ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload2/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload2/_latestVersion)

```groovy
	dependencies{
   		 compile 'zlc.season:rxdownload2:1.1.1'
         //or
         compile 'zlc.season:rxdownload2:2.0.0-beta2' //测试版
	}
```

3.配置权限

```xml
 	<!-- 在XML中设置权限 -->
	<uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

> **注意: Android 6.0 以上还必须申请运行时权限, 如果遇到不能下载, 请先检查权限**

#### 二、创建实例及配置

1.创建RxDownload实例

```java
RxDownload rxDownload = RxDownload.getInstance(context);  //单例
```

2.参数配置，可以配置的参数如下:

```java
RxDownload.getInstance(context)
    .retrofit(myRetrofit)             //若需要自己的retrofit客户端,可在这里指定
    .defaultSavePath(defaultSavePath) //设置默认的下载路径
    .maxThread(3)                     //设置最大线程
    .maxRetryCount(3)                 //设置下载失败重试次数
    .maxDownloadNumber(5)             //Service同时下载数量
    ...
```

#### 三、开始下载

1.Normal Download



3.下载参数说明

- ```download(String url)```  只传url时，会自动从服务器获取文件名
- ```download(String url, String saveName)``` 也可手动指定保存的文件名称
- ```download(String url,String saveName,String savePath)``` 手动指定文件名和保存路径
- ```download(DownloadBean bean)```  若需要保存额外的数据到数据库，可以手动构造Download Bean，具体细节请查看源码

4.取消或暂停下载

**取消订阅, 即可暂停下载**

```java
Disposable disposable = RxDownload.getInstance(context)
                .download(url)
  				//...

//取消订阅, 即可暂停下载, 若服务端不支持断点续传,下一次下载会重新下载,反之会继续下载
if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
}
```

5.提供了一个transferform方式供RxJava的Compose操作符使用

例如与RxPermission结合使用

> RxPermission是为Android 6.0解决运行时权限的一个库, 这里是该库的地址: [RxPermission](https://github.com/tbruyelle/RxPermissions)

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
                    .compose(RxDownload.getInstance(context).<Boolean>transform(url))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<DownloadStatus>() { ... });
```

#### 三、Service下载

- 使用Service进行下载, 具备后台下载能力
- 取消订阅不会导致下载暂停
- 能够实时获取下载进度
- 同时保存下载记录到数据库
- 能够设置最大下载数量, 当添加任务到下载队列时, 多余的下载任务将等待, 直到可以下载的时候自动开始下载.

1.开始下载, 添加到下载队列中. 

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

**serviceDownload()不再使用广播的方式,也不再接收下载进度, 因此无需异步操作, 也无需取消订阅.**

2.接收下载事件和下载状态.  取消订阅即可取消接收. 

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

- 不管任务是否开始下载, 都能获取到该url对应的事件和状态.
- 无需再单独从数据库中读取下载记录了.
- 注意, 现在接受下载进度不会再收到onError事件和onComplete事件了, 都放在DownloadEvent中了.

3.下载事件DownloadEvent说明

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

DownloadEvent中添加了一个flag标记, 用于标记当前下载任务处于什么状态, 有以下状态:

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

当在onNext(DownloadEvent event)中接收到event时,可根据flag的状态来判断当前下载任务处于何种状态, 从而进行不同的操作.

4.暂停下载, 暂停下载地址为url的下载任务

```java
 mRxDownload.pauseServiceDownload(url).subscribe();
```

5.取消下载,取消下载地址为url的下载任务

```java
 mRxDownload.cancelServiceDownload(url).subscribe();
```

6.删除下载, 取消该下载任务并从数据库中删除该任务

**可选是否删除下载的文件.**

```java
mRxDownload.deleteServiceDownload(url,true or false).subscribe();
```

7.获取所有的下载记录, 获取数据库中所有的下载记录

```java
mRxDownload.getTotalDownloadRecords()
                .subscribe(new Action1<List<DownloadRecord>>() {
                    @Override
                    public void call(List<DownloadRecord> list) {
                        mAdapter.addAll(list);
                    }
                });
```

8.获取下载的文件

```java
File file = mRxDownload.getRealFiles(saveName, defaultPath)[0];
```

9.更多功能后续将会逐步完善

若您对此项目有疑问,欢迎来提issues.

### 关于我

若您想对该项目来进行交流,可以通过以下方式:

QQ : 270362455

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