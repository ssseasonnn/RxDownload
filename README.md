# RxDownload

![](https://img.shields.io/badge/language-kotlin-brightgreen.svg) ![](https://img.shields.io/badge/RxJava-2.0-blue.svg)

A multi-threaded download tool written with RxJava and Kotlin

*Read this in other languages: [中文](README.ch.md), [English](README.md)* 

## How to Use

### Preparation

1.Add Gradle dependencies[ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload3/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload3/_latestVersion)

```groovy
dependencies{
    compile 'zlc.season:rxdownload3:x.y.z'
}
```

2.Configure the permissions

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

> **Please note that Android 6.0 and above must also apply run-time permissions, if you are unable to download, check permissions**

### Download

1.Create a mission

Create a mission and receive the status of the download

```java
val disposable = RxDownload.create(mission)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    setProgress(status)
                    setActionText(status)
                }
```

> Note: The download status is also received here, the received status will be automatically updated according to the different download status.
> Repeated calls **DO NOT** cause the task to be created more than once, so you can call this method wherever you want to receive the state to receive the downloaded state.

2.Start download

```java
RxDownload.start(mission).subscribe()
```

3.Stop download

```java
RxDownload.stop(mission).subscribe()
```

> Just three steps that is so simple !!


### AutoStart

There are two simple ways:

- Add the autoStart parameter to the **create** method，this only take effect for the current mission.

```kotlin
RxDownload.create(mission,autoStart)
       .subscribe{
           ...
       }
```

- Enable AutoStart config, this will take effect for all missions.
```java
DownloadConfig.Builder.create(context)
                  .enableAutoStart(true)
                  ...
                  
                  
DownloadConfig.init(builder)
```

For more APIs please move RxDownload.kt

### Configuration

Add your configuration when APP starts up, like this:

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

Have a wealth of configuration options to meet your needs:

```java
DownloadConfig.Builder.create(this)
                .setDefaultPath("custom download path")     //Set the default download address
                .enableDb(true)     //Enable the database
                .setDbActor(CustomSqliteActor(this))    //Customize the database
                .enableService(true)    //Enable Service
                .useHeadMethod(true)    //Use http HEAD method.
                .setMaxRange(10)       // Maximum concurrency for each mission.
                .setRangeDownloadSize(4*1000*1000) //The size of each Range，unit byte
                .setMaxMission(3)      // The number of mission downloaded at the same time
                .enableNotification(true)   //Enable Notification
                .setNotificationFactory(NotificationFactoryImpl())      //Custom notification
                .setOkHttpClientFacotry(OkHttpClientFactoryImpl())      //Custom OKHTTP
                .addExtension(ApkInstallExtension::class.java)    //Add extension
```

### Extension

Customize your exclusive operation

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

> Refer to the ApkInstallExtension code

### Proguard

No special proguard, just add Retrofit and OKHTTP can be proguard

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