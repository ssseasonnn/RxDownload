# RxDownload
The download tool based on RxJava . Support multi-threaded download and breakpoint download, intelligent judge whether to support multi-threaded download and breakpoint download.
基于RxJava打造的下载工具, 支持多线程下载和断点续传, 智能判断是否支持断点续传等功能

标签（空格分隔）： Android  RxJava  Download Tools Multi-threaded



---

## 基于RxJava打造的下载工具, 支持多线程和断点续传,同时具备后台下载的能力
## Download tool based on RxJava. Support multi-thread and breakpoint download. Also have background download ability.

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

### 使用方式

#### 准备工作

1.添加Gradle依赖

[![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload/images/download.svg)](https://bintray.com/ssseasonnn/android/RxDownload/_latestVersion)

```groovy
	dependencies{
   		 compile 'zlc.season:rxdownload:1.2.0'
	}
```

2.配置权限

```xml
 	<!-- 在XML中设置权限 -->
	<uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

> **注意: Android 6.0 以上还必须申请运行时权限, 如果遇到不能下载, 请先检查权限**

#### 常规下载

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
                .maxThread(10)     //设置最大线程
                .maxRetryCount(10) //设置下载失败重试次数
                .retrofit(myRetrofit)//若需要自己的retrofit客户端,可在这里指定
                .defaultSavePath(defaultSavePath)//设置默认的下载路径
                .download(url,savename,savepath) //开始下载
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {);
```

 **Tips:  RxDownload.getInstance() 每次返回的是一个全新的对象, 因此创建多个下载任务时应该避免多次创建实例:**   

```java
RxDownload rxDownload = RxDownload.getInstance()
  					   .maxThread(10)
  					   .maxRetryCount(10)
  					   .defaultSavePath(defaultPath);
//download task 1: 
Subscription subscription1 = rxDownload.download(url1,name1,null)...
//download task 2:  
Subscription subscription2 = rxDownload.download(url2,name2,null)...  
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

#### Service下载

- 使用Service进行下载, 具备后台下载能力
- 取消订阅不会导致下载暂停
- 能够实时获取下载进度
- 同时保存下载记录到数据库

1.开始下载, 插入下载记录到数据库中, 同时标记下载状态为STARTED

```java
 Subscription subscription = RxDownload.getInstance().context(this)//使用service下载需Context
                .serviceDownload(url,saveName,null)
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        //下载完成,同时会在数据库中标记下载状态为COMPLETED
                    }

                    @Override
                    public void onError(Throwable e) {
					  //下载失败,同时会在数据库中标记下载状态为FAILED
                    }

                    @Override
                    public void onNext(DownloadStatus status) {

                    }
                });
```

**serviceDownload()会同时注册广播接收器用于接收下载进度.**

**取消订阅不会暂停下载. 取消订阅即可取消注册广播**

**这里有另一个不注册广播接收器的版本serviceDownloadNoReceiver().**

2.获取下载进度

如果当前任务正在下载, 注册广播接收当前的下载进度

```java
 Subscription temp = mRxDownload.registerReceiver(url) //获取下载链接为url的下载进度.
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                      
                    }

                    @Override
                    public void onError(Throwable e) {
                      
                    }

                    @Override
                    public void onNext(final DownloadStatus status) {
                      
                    }
                });
// 取消订阅即可取消注册广播
```

3.读取下载记录

从数据库中读取该下载记录, 记录包括下载进度, 下载状态,下载时间等信息

```java
 Subscription query = mRxDownload.getDownloadRecord(url) //获取下载地址为url的下载记录
                .subscribe(new Action1<DownloadRecord>() {
                    @Override
                    public void call(DownloadRecord record) {
                        //如果有下载记录才会发送事件
                        //根据下载记录设置当前的状态
                        mProgress.setIndeterminate(record.getStatus().isChunked);
                        mProgress.setMax((int) record.getStatus().getTotalSize());
                        mProgress.setProgress((int) record.getStatus().getDownloadSize());
                        mPercent.setText(record.getStatus().getPercent());
                        mSize.setText(record.getStatus().getFormatStatusString());

                        int flag = record.getDownloadFlag();
                        //设置下载状态
                        mDownloadController.setStateAndDisplay(flag);
                    }
                });
```

4.暂停下载, 暂停下载地址为url的下载任务, 同时在数据库中标记为PAUSED

```java
 Subscription subscription = mRxDownload.pauseServiceDownload(url)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                       //下载暂停, do something
                        mDownloadController.setStateAndDisplay(DownloadFlag.PAUSED);
                    }
                });
```

5.取消下载,取消下载地址为url的下载任务,同时在数据库中标记为CANCELED

```java
 Subscription subscription = mRxDownload.cancelServiceDownload(url)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                      //下载取消, do something
                        mDownloadController.setStateAndDisplay(DownloadFlag.CANCELED);
                    }
                });
```

6.删除下载, 取消该下载任务并从数据库中删除该任务

```java
  Subscription subscription = mRxDownload.deleteServiceDownload(mData.mRecord.getUrl())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                    }
                });
```

7.获取所有的下载记录, 获取数据库中所有的下载记录

```java
  Subscription subscription =mRxDownload.getTotalDownloadRecords()
                .subscribe(new Action1<List<DownloadRecord>>() {
                    @Override
                    public void call(List<DownloadRecord> list) {
                        mAdapter.addAll(list);
                    }
                });
```

8.更多功能后续将会逐步完善

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