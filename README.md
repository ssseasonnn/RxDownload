# RxDownload
The download tool based on RxJava . Support multi-threaded download and breakpoint download, intelligent judge whether to support multi-threaded download and breakpoint download.


基于RxJava打造的下载工具, 支持多线程下载和断点续传, 智能判断是否支持断点续传等功能

标签（空格分隔）： Android  RxJava  Download Tools Multi-threaded



---

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


### 2016-11-7更新:

- 修复自定义路径不能下载的bug

### 2016-11-9 更新

- 新增transform方法, 可使用RxJava的compose操作符组合调用下载,具体使用方式请看文章底部

###2016-11-17 更新
- v1.2.0  发布
- 支持后台下载
- 支持获取下载进度
- 具备下载管理功能
- 使用方式请查看文档


### 2016-11-24 更新

- 取消上一版本使用的广播
- 简化后台下载的使用方式
- 后台下载支持设置最大下载任务数量, 其余下载任务等待
- 修复常规下载中, 同一url能够多次下载的BUG
- 使用方式请看文档

### 2016-11-25 更新

- 修复几个BUG
- 新增APk下载完成自动安装功能, 可在参数配置中配置
- 使用方式请下载demo.


### 2016-12-1 更新

- 发布基于RxJava2修改的版本RxDownload2, 使用方式请看文档
- 新版本在rxjava2分支中, 若需查看源码请git clone后切换至该分支查看.
- PS: 注意最好不要用网页上的download下载源码, 可能出现未知的错误.


### 2016-12-8 更新

- 修复chunked方式不能正常下载的bug


### 2016-12-12 更新

- 修复文件删除仍然显示下载完成的bug
- 移除Manifest中多余的配置


### 2016-12-13 更新

- 修复LastModify日期bug


### 2016-12-14 更新

- 修复service下载接收下载进度中, 当收到error事件或者Complete事件之后, 就不再收到其余事件的BUG
- 在本次修改中取消掉了receiveDownloadStatus()中的onError()和onComplete(). 
- 也就是说接收下载进度过程中不会再调用onError和onComplete,  所有的处理都放到了onNext中.
- 在DownloadEvent中增加了mError属性,  当收到DownloadFlag = Failed时, 就可以从event中取出异常信息进行处理. 
- 具体信息请仔细阅读文档.

### 2016-12-29 更新

- 修复下载进度可能超过100%的BUG
- 修复其余一些BUG



### 效果图


<figure class="third">
    <img title="普通下载" width="300" src="https://github.com/ssseasonnn/RxDownload/blob/master/gif/basic_download.gif?raw=true">
    <img title="Service下载" width="300" src="https://github.com/ssseasonnn/RxDownload/blob/master/gif/service_download.gif?raw=true">
    <img title="下载管理" width="300" src="https://github.com/ssseasonnn/RxDownload/blob/master/gif/download_manager.gif?raw=true">
</figure>


### 下载流程图

<figure class="third">
	<img src="https://github.com/ssseasonnn/RxDownload/blob/master/download.png?raw=true" title="下载流程图">
</figure>



# 注意,请仔细阅读这行文字:

Demo中所有的下载链接均是从网上随意找的, 经常会出现地址失效或者下载失败等等各种错误, 如果你下载Demo运行发现结果不对, 请先行替换下载链接进行测试.

### 使用方式

#### 一、准备工作

1.添加Gradle依赖

 [ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload/_latestVersion)

```groovy
	dependencies{
   		 compile 'zlc.season:rxdownload:1.2.8'
	}
```

2.For RxJava2

RxDownload 现在支持RxJava2, 只需将包名改为 ```zlc.season.rxdownload2.``` 

[ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload2/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload2/_latestVersion)

```groovy
	dependencies{
   		 compile 'zlc.season:rxdownload2:1.0.6'
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

#### 二、常规下载

- 不具备后台下载能力
- 取消订阅即暂停下载. 

1.使用方式

```java
Subscription subscription = RxDownload.getInstance()
                .download(url, "weixin.apk", null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                     @Override
                    public void onCompleted() {
					//下载完成
                    }

                    @Override
                    public void onError(Throwable e) {
					//下载出错
                    }

                    @Override
                    public void onNext(final DownloadStatus status) {
					//下载状态
                    }
                });
```

> 参数说明: 参数分别为下载地址,保存文件名,保存地址.
>
> url与saveName为必传参数, savePath为可选参数, 默认的下载地址为/storage/emulated/0/Download/目录下, 也就是内置存储的Download目录

2.参数配置

**可以配置的参数如下:**

```java
Subscription subscription = RxDownload.getInstance()
                .maxThread(10)                    //设置最大线程
                .maxRetryCount(10)                //设置下载失败重试次数
                .retrofit(myRetrofit)             //若需要自己的retrofit客户端,可在这里指定
                .defaultSavePath(defaultSavePath) //设置默认的下载路径
                .context(this)                    //自动安装需要Context
                .autoInstall(true);               //下载完成自动安装
                .download(url,savename,savepath)  //开始下载
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {);
```

**Tips: **

- RxDownload.getInstance() 每次返回的是一个全新的对象. 
- 每个实例都可以单独设置最大线程, 默认路径等参数.
- 因此创建多个下载任务时应该避免多次创建实例.

```java
RxDownload rxDownload1 = RxDownload.getInstance()
  					   .maxThread(5) 
  					   .maxRetryCount(10)
  					   .defaultSavePath(defaultPath);
//download task 1: 
Subscription subscription1 = rxDownload1.download(url1,name1,null)...
//download task 2:  
Subscription subscription2 = rxDownload1.download(url2,name2,null)...  

RxDownload rxDownload2 = RxDownload.getInstance()
  					   .maxThread(10)...
//download task 3:  
Subscription subscription3 = rxDownload2.download(url3,name3,null)...   
```

3.取消或暂停下载

**取消订阅, 即可暂停下载**

```java
Subscription subscription = RxDownload.getInstance()
                .download(url, null, null)
  				//...

//取消订阅, 即可暂停下载, 若服务端不支持断点续传,下一次下载会重新下载,反之会继续下载
if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
}
```

4.提供了一个transferform方式供RxJava的Compose操作符使用

例如与RxPermission结合使用

> RxPermission是为Android 6.0解决运行时权限的一个库, 这里是该库的地址: [RxPermission](https://github.com/tbruyelle/RxPermissions)

```java
 subscription =  RxPermissions.getInstance(mContext)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE) //申请存储卡权限
                    .doOnNext(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean granted) {
                            if (!granted) {  //权限被拒绝
                                throw new RuntimeException("no permission");
                            }
                        }
                    })
                    .observeOn(Schedulers.io())
                    .compose(RxDownload.getInstance().transform(data.url, data.name, null))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<DownloadStatus>() { ... });
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
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        Toast.makeText(ServiceDownloadActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
                    }
                });
```

**serviceDownload()不再使用广播的方式,也不再接收下载进度, 因此无需异步操作, 也无需取消订阅.**

2.接收下载事件和下载状态.  取消订阅即可取消接收. 

```java
 Subscription temp =  mRxDownload.receiveDownloadStatus(url)
                .subscribe(new Action1<DownloadEvent>() {
                    @Override
                    public void call(DownloadEvent event) {
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

```java
mRxDownload.deleteServiceDownload(url).subscribe();
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