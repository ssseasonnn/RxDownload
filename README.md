# RxDownload

![](https://img.shields.io/badge/language-kotlin-brightgreen.svg) ![](https://img.shields.io/badge/RxJava-2.0-blue.svg)

A multi-threaded download tool written with RxJava and Kotlin

*Read this in other languages: [中文](README.ch.md), [English](README.md)*

## How to Use

### Preparation

Step 1. Add the dependency[ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload3/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload3/_latestVersion)

```groovy
dependencies{
    implementation 'zlc.season:rxdownload3:x.y.z'
}
```

Step 2. Add permissions

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### Usage

Step 1. Create a mission

```kotlin
val mission = Mission(URL, FILE_NAME, SAVE_PATH, IS_OVERRIDE)
```


Step 2. Subscribe to the mission updates

```kotlin
val disposable = RxDownload.create(mission, autoStart = false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { progress ->

                }
```

> Note: The mission status is received here
> Repeated calls **DO NOT** cause the task to be created more than once
> Don't forget to dispose `disposable` in `onDestroy()` or similar method (To dispose call `dispose(disposable)` )


Step 3. Start download

```kotlin
RxDownload.start(mission).subscribe()
```

Stop download

```kotlin
RxDownload.stop(mission).subscribe()
```


### AutoStart Download

There are two simple ways:

- Add the autoStart parameter to the **create** method，this only take effect for the current mission.

```kotlin
RxDownload.create(mission,autoStart)
       .subscribe{
           ...
       }
```

- Enable AutoStart config, this will take effect for all missions.
```kotlin
DownloadConfig.Builder.create(context)
                  .enableAutoStart(true)

DownloadConfig.init(builder)
```

For more APIs please see RxDownload.kt


### Override
You can configure `Mission` so that if file exists it will be re-downloaded.
If not specified - false

```kotlin
val mission = Mission(...)
mission.overwrite = Boolen (true/false)
```


### Configuration (optional)

Add your configuration when APP starts up, like this:

```kotlin
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

or

```kotlin
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

```kotlin
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

Add Retrofit and OKHTTP can be proguard

```gradle
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
