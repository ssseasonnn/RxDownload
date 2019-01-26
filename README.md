# RxDownload

A multi-threaded download tool written with RxJava and Kotlin

## How to Use

### Preparation

Step 1. Add the JitPack repository to your build file

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

```gradle
dependencies{
    implementation 'com.github.Deishelon:RxDownload:Tag'
}
```

Step 3. Add permissions

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
                .subscribe { status ->

                }
```

> Note: The mission status is received here
> Repeated calls **DO NOT** cause the task to be created more than once
> Don't forget to dispose `disposable` in `onDestroy()` or similar method (To dispose call `dispose(disposable)` )


Step 3. Start download

```java
RxDownload.start(mission).subscribe()
```

Stop download

```java
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
