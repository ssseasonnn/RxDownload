# RxDownload

基于RxJava打造的下载工具, 支持多线程下载和断点续传, 智能判断是否支持断点续传等功能

*Read this in other languages: [中文](README.md), [English](README.en.md)* 


## 更新日志：

[更新日志搬到这里了](https://github.com/ssseasonnn/RxDownload/blob/master/CHANGE_LOG.md)


## 效果图


<img title="普通下载" width="30%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/basic_download.gif">
<img title="Service下载" width="30%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/service_download.gif">
<img title="下载管理"  width="33%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/download_manager.gif">



## 使用方式

### 准备工作

1.添加Gradle依赖[ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload2/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload2/_latestVersion)

```groovy
	dependencies{
         compile 'zlc.season:rxdownload2:2.0.0-beta3'
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

### 创建实例及配置

1.创建RxDownload实例

```java
RxDownload rxDownload = RxDownload.getInstance(context);  //单例
```

2.参数配置，可以配置的参数如下:

```java
RxDownload rxDownload = RxDownload.getInstance(context)
    .retrofit(myRetrofit)             //若需要自己的retrofit客户端,可在这里指定
    .defaultSavePath(defaultSavePath) //设置默认的下载路径
    .maxThread(3)                     //设置最大线程
    .maxRetryCount(3)                 //设置下载失败重试次数
    .maxDownloadNumber(5)             //Service同时下载数量
    ...
```

3.下载参数说明

- ```xxDownload(String url)```  当只传url时，会自动从服务器获取文件名
- ```xxDownload(String url, String saveName)``` 也可手动指定保存的文件名称
- ```xxDownload(String url,String saveName,String savePath)``` 手动指定文件名和保存路径
- ```xxDownload(DownloadBean bean)```  当需要保存额外信息到数据库时，可以手动构造Download Bean，具体细节请查看源码

### 开始下载

#### Normal download

- 常规下载，不具备后台下载能力
- 适合轻量下载

1.调用方式

```java
Disposable disposable = RxDownload.getInstance(this)
        .download(url)                       //只传url即可
        .subscribeOn(Schedulers.io()) 
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<DownloadStatus>() {
            @Override
            public void accept(DownloadStatus status) throws Exception {
                //DownloadStatus为下载进度
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                //下载失败
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                //下载成功
            }
        });
```

2.暂停下载

```java
//获得订阅返回的Disposable.
Disposable disposable = RxDownload.getInstance(context)
                .download(url)...
  				
//取消订阅, 即可暂停下载
if (disposable != null && !disposable.isDisposed()) {
    disposable.dispose();
}
```

3.继续下载

```java
//重新调用download()方法，传入相同的url即可
//若该url支持断点续传则继续下载，若不支持则重新下载
Disposable disposable = RxDownload.getInstance(context)
                .download(url)...
```

4.transferform形式

提供给RxJava的Compose操作符使用，例如与RxPermission结合使用

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
            .compose(RxDownload.getInstance(context).<Boolean>transform(url))  //download
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<DownloadStatus>() { ... });
```

#### Service Download

- 使用Service进行下载, 具备后台下载能力
- 具备下载管理功能，能设置同时下载数量
- 能够批量添加下载任务

1.添加单个任务

```java
RxDownload.getInstance(this)
        .serviceDownload(url)   //只需传url即可，添加一个下载任务
        .subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                Toast.makeText(MainActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.w(TAG, throwable);
                Toast.makeText(MainActivity.this, "添加任务失败", Toast.LENGTH_SHORT).show();
            }
        });
//只是添加下载任务到队列中，因此不需要取消订阅，取消订阅也不会导致下载暂停
```

2.添加多个任务

```java
//批量下载
RxDownload.getInstance(this)
		.serviceMultiDownload(missionId, url1, url2, url3)  //添加三个任务
        .subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                Toast.makeText(MainActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                 Log.w(TAG, throwable);
                Toast.makeText(MainActivity.this, "添加任务失败", Toast.LENGTH_SHORT).show();
            }
        });
//需要missionId，可以是任意字符串
//可通过该missionId暂停或删除该批量下载的所有任务
//可通过该missionId查询该批量下载的所有任务的下载情况
```

3.接收下载事件和下载状态.  

```java
//接收事件可以在任何地方接收，不管该任务是否开始下载均可接收.
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
// 在Activity销毁时取消订阅，取消订阅即可取消接收事件，但并不会暂停下载.
// 不管任务是否开始下载, 都能获取到该url对应的事件和状态.
// 只会收到onNext事件，不会收到onError和onComplete事件，因此只需监听onNext即可.
```

4.暂停下载

```java
//单一暂停，暂停地址为url的下载任务
rxDownload.pauseServiceDownload(url).subscribe();

//批量暂停，暂停该missionId代表的所有任务
rxDownload.pauseServiceDownload(missionId).subscribe();
```

5.继续下载

```java
//再次调用下载方法并传入相同的url即可继续下载
RxDownload.getInstance(this)
        .serviceDownload(url) 
        ...
```

6.删除下载

```java
//暂停地址为url的下载并从数据库中删除记录，deleteFile为true会同时删除该url下载产生的所有文件
rxDownload.deleteServiceDownload(url, deleteFile).subscribe();

//批量删除，暂停该missionId代表的所有任务，同时删除所有任务的记录
rxDownload.deleteServiceDownload(missionId,deleteFile).subscribe();
```

7.transferform形式

```java
//single url
.compose(rxDownload.<Object>transformService(url))

//multi url
.compose(rxDownload.<Object>transformMulti(missionId,url1,url2,url3)) 
```

### 获取下载记录

获取数据库中所有的下载记录

```java
mRxDownload.getTotalDownloadRecords()
    .subscribe(new Action1<List<DownloadRecord>>() {
        @Override
        public void call(List<DownloadRecord> list) {
            mAdapter.addAll(list);
        }
    });
```

### 获取下载文件

获取下载的文件

```java
//利用url获取
File[] files = rxDownload.getRealFiles(url);
if (files != null) {
	File file = files[0];
}
//利用saveName及savePath获取
File file = rxDownload.getRealFiles(saveName,savePath)[0];
```

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