# RxDownload

![](https://img.shields.io/badge/language-kotlin-brightgreen.svg) ![](https://img.shields.io/badge/RxJava-2.0-blue.svg)

基于RxJava打造的下载工具, 支持多线程下载和断点续传,使用Kotlin编写

*Read this in other languages: [中文](README.ch.md), [English](README.md)* 

## 使用方式

### 准备

1.添加Gradle依赖[ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload3/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload3/_latestVersion)

```groovy
dependencies{
    compile 'zlc.season:rxdownload3:x.y.z'
}
```

2.配置权限

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

> **注意: Android 6.0 以上还必须申请运行时权限, 如果遇到不能下载, 请先检查权限**

### 下载

1.创建任务

创建一个任务，并且接收下载的状态

```java
val disposable = RxDownload.create(mission)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    setProgress(status)
                    setActionText(status)
                }
```

> 注意：下载状态的接收也是在这里进行，接收到的status会根据不同的下载状态自动更新。
> 重复调用**不会**导致任务多次创建，因此可以在任何想要接收状态的地方调用该方法来接收下载的状态。

2.开始下载

```java
RxDownload.start(mission).subscribe()
```

3.停止下载

```java
RxDownload.stop(mission).subscribe()
```

> 只需三步, 就是这样简单!!

### AutoStart

两种方式:

- 传入autoStart参数，只对当前任务生效

```kotlin
RxDownload.create(mission,autoStart)
       .subscribe{
           ...
       }
```

- 启用autoStart配置，对所有任务生效
```java
DownloadConfig.Builder.create(context)
                  .enableAutoStart(true)
                  ...
                  
                  
DownloadConfig.init(builder)
```

更多API请移步RxDownload.kt

### 配置

在APP启动时添加您的配置,就像这样:

```java
class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val builder = DownloadConfig.Builder.create(this)
                .enableDb(true)
                .enableNotification(true)
				...

        DownloadConfig.init(builder)
    }
}
```

拥有丰富的配置选项满足您的需求:

```java
DownloadConfig.Builder.create(this)
                .enableAutoStart(true)              //自动开始下载
                .setDefaultPath("custom download path")     //设置默认的下载地址
                .useHeadMethod(true)    //使用http HEAD方法进行检查
                .setMaxRange(10)       // 每个任务并发的线程数量
                .setRangeDownloadSize(4*1000*1000) //每个线程下载的大小，单位字节
                .setMaxMission(3)      // 同时下载的任务数量
                .enableDb(true)                             //启用数据库
                .setDbActor(CustomSqliteActor(this))        //自定义数据库
                .enableService(true)                        //启用Service
                .enableNotification(true)                   //启用Notification
                .setNotificationFactory(NotificationFactoryImpl()) 	    //自定义通知
                .setOkHttpClientFacotry(OkHttpClientFactoryImpl()) 	    //自定义OKHTTP
                .addExtension(ApkInstallExtension::class.java)          //添加扩展
```

### 扩展

定制您的专属操作

```java
class CustomExtension:Extension {
    override fun init(mission: RealMission) {
        //Init
    }

    override fun action(): Maybe<Any> {
        //Your action
    }
}
```

> 可参考ApkInstallExtension代码

### 混淆

无特殊混淆, 只需添加Retrofit及OKHTTP的混淆即可

```groovy
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature
-keepattributes Exceptions

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
```

### License

> ```
> Copyright 2017 Season.Zlc
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