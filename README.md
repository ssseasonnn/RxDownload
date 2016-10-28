# RxDownload
基于RxJava打造的下载工具, 支持多线程下载和断点续传, 智能判断是否支持断点续传等功能

# 造轮子 - RxDownload

标签（空格分隔）： Android  RxJava  Download Tools



---

## 基于RxJava打造的下载工具, 支持多线程和断点续传


### 效果图

![demo](https://github.com/ssseasonnn/RxDownload/blob/master/demo.gif)



### 主要功能:

- 使用Retrofit+OKHTTP来进行网络请求
- 基于RxJava打造, 支持RxJava各种操作符链式调用
- 断点续传, 根据服务端响应值自动判断是否支持断点续传
- 若不支持断点续传,则使用单线程进行传统下载
- 多线程下载, 可以设置最大线程, 默认值为3
- 检测到网络连接失败自动尝试重连, 并可配置最大重试次数,默认值为3





### 使用方式

1.添加Gradle依赖

[![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload/images/download.svg)](https://bintray.com/ssseasonnn/android/RxDownload/_latestVersion)

```groovy
	dependencies{
   		 compile 'zlc.season:rxdownload:1.0.0'
	}
```

2.代码调用

```java
Subscription subscription = RxDownload.getInstance()
                .download(url, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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
```

> download(String url, String  saveName, String savePath)参数说明:
>
> 参数分别为下载地址,保存文件名,保存地址, 文件名和保存地址传null,则使用默认的文件名和默认的下载地址.
>
> 默认的文件名先从响应头的content-disposition字段中获取, 若没有该字段,则截取url的最后一段当作文件名.
>
> 例如下载地址为: http://a.gdown.baidu.com/data/wisegame/f4314d752861cf51/WeChat_900.apK , 若content-disposition字段不存在, 则截取WeChat_900.apk作为文件名. 若都获取不到,则会抛出异常提示, 需要手动指定文件名.
>
> 默认的下载地址为/SdCard/Download/目录

3.参数配置

可以配置的参数如下:

```java
Subscription subscription = RxDownload.getInstance()
                .maxThread(10)     //设置最大线程
                .maxRetryCount(10) //设置下载失败重试次数
                .retrofit(myRetrofit)//若需要自己的retrofit客户端,可在这里指定
                .defaultSavePath(defaultSavePath)//设置默认的下载路径
                .download(url,savename,savepath) //开始下载
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(DownloadStatus status) {

                    }
                });
```

4.DownloadStatus 下载状态

```java
class DownloadStatus {
    private long totalSize;
    private long downloadSize;
    public boolean isChunked = false;
    //...
    //返回文件总大小,单位为byte
    public long getTotalSize() {}

    //返回已下载总大小,单位为byte
    public long getDownloadSize() {}

    //返回格式化的总大小,如:10MB
    public String getFormatTotalSize() {}

	//返回格式化的已经下载的大小,如:5KB
    public String getFormatDownloadSize() {}

    //返回格式化的状态字符串,如:2MB/36MB
    public String getFormatStatusString() { }

    //返回下载的百分比, 保留两位小数,如:5.25%
    public String getPercent() {}
}

```

> onNext 中的DownloadStatus表示的就是当前的下载进度

5.注意事项

- 调用下载后会返回Subscription, 一定要在Activity或者Fragment销毁之前取消订阅,否则会造成内存泄漏, 与平常RxJava的使用方式一致.

```java
Subscription subscription = RxDownload.getInstance()
                .download(url, null, null)
  				//...

//取消订阅
if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
}
```

- 关于判断是否支持断点续传, 首先在第一次请求头中加上Range:bytes=0- , 然后根据服务端的响应头判断是否有Content-Range: bytes 0-38786195/38786196字段, 若有则使用多线程+断点续传下载,反之使用单线程下载
- 关于断点续传判断服务端文件改变的情况, 由于许多服务器并不返回ETAG和LastModify字段, 所以为了更通用一点,没有采用这种方式进行判断,而是采用了一种简单粗暴的方法:  根据文件大小来进行判断,  这种方法可能会导致bug,但几率很小.

6.更多功能后续将会逐步完善

若您对此项目有疑问,欢迎来提issues.

### 关于我

若您想对该项目来进行交流,可以通过以下方式:

QQ : 270362455

Gmail: ssseasonnn@gmail.com

### License

> ```
> Copyright 2015 Season.Zlc
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