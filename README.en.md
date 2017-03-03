# RxDownload
The download tool based on RxJava . Support multi-threaded download and breakpoint download, intelligent judge whether to support multi-threaded download and breakpoint download.

*Read this in other languages: [中文](README.md), [English](README.en.md)*

---

## Update Log:：

[Update log moved here](https://github.com/ssseasonnn/RxDownload/blob/master/CHANGE_LOG.md)


## Effect diagram


<img title="普通下载" width="30%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/basic_download.gif">
<img title="Service下载" width="30%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/service_download.gif">
<img title="下载管理"  width="33%" src="https://raw.githubusercontent.com/ssseasonnn/RxDownload/master/gif/download_manager.gif">

## How to Use

### 1. Preparation

1.Add Gradle dependencies[ ![Download](https://api.bintray.com/packages/ssseasonnn/android/RxDownload2/images/download.svg) ](https://bintray.com/ssseasonnn/android/RxDownload2/_latestVersion)

```groovy
	dependencies{
         compile 'zlc.season:rxdownload2:2.0.0-beta3'
	}
```


2.Configure the permissions

```xml
	<uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

> **Please note that Android 6.0 and above must also apply run-time permissions, if you are unable to download, check permissions**

### Create instances and configurations

1.Create RxDownload instance

```java
RxDownload rxDownload = RxDownload.getInstance(context);  //Single instance
```

2.Parameter configuration, you can configure the parameters are as follows:

```java
RxDownload rxDownload = RxDownload.getInstance(context)
    .retrofit(myRetrofit)    //If you need your own retrofit client, you can specify here
    .defaultSavePath(defaultSavePath) //Set the default download path
    .maxThread(3)                     //Set the max thread
    .maxRetryCount(3)                 //Set the max retry count
    .maxDownloadNumber(5)             //Set the max download number when  service download.
    ...
```

3.Download method parameter description

- ```xxDownload(String url)```  

  When only pass url, will automatically get the file name from the server

- ```xxDownload(String url, String saveName)``` 

  You can also specify the name of the saved file

- ```xxDownload(String url,String saveName,String savePath)```

   You can also specify the file name and save the path manually

- ```xxDownload(DownloadBean bean)``` 

  When you need to save additional information to the database, you can manually construct Download Bean, the specific details, please see the source code.

### Start Download

#### Normal download

- Regular download, do not have the ability to download the background
- Suitable for lightweight download

1.Call the way

```java
Disposable disposable = RxDownload.getInstance(this)
        .download(url)                       //just pass url
        .subscribeOn(Schedulers.io()) 
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<DownloadStatus>() {
            @Override
            public void accept(DownloadStatus status) throws Exception {
                //DownloadStatus is download progress
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                //download failed
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                //download success
            }
        });
```

2.Pause Download

```java
//Get the  Disposable.
Disposable disposable = RxDownload.getInstance(context)
                .download(url)...
  				
//Dispose this Disposable to pause the download
if (disposable != null && !disposable.isDisposed()) {
    disposable.dispose();
}
```

3.Continue to download

```java
//Re-call the download () method, and pass the same url.
//If the url support for breakpoint download, continue to download, if not support to re-download
Disposable disposable = RxDownload.getInstance(context)
                .download(url)...
```

4.Form of transferform

The Compose operator provided to RxJava is used, for example, in conjunction with RxPermission

> RxPermission is a library for the run-time permissions for Android 6.0, here is the address of the library:[RxPermission](https://github.com/tbruyelle/RxPermissions)

```java
disposable =  RxPermissions.getInstance(mContext)
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE) //Apply for memory card permissions
            .doOnNext(new Consumer<Boolean>() {
                @Override
                public void accept(Boolean granted)  throws Exception {
                    if (!granted) {  //Permission is denied
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

- Use Service to download, with background download capabilities
- With the download management function, can set the number of simultaneous downloads
- Ability to add download tasks in bulk

1.Add a single task

```java
RxDownload.getInstance(this)
        .serviceDownload(url)   //Just pass url, add a download task
        .subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                Toast.makeText(MainActivity.this, "Start download", Toast.LENGTH_SHORT).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.w(TAG, throwable);
                Toast.makeText(MainActivity.this, "Failed to add task", Toast.LENGTH_SHORT).show();
            }
        });
//Just add the download task to the queue, so there is no need to dispose the Disposable, dispose the Disposable will not lead to download pause
```

2.Add multiple tasks

```java
//Batch download
RxDownload.getInstance(this)
		.serviceMultiDownload(missionId, url1, url2, url3)  //Add three tasks
        .subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                Toast.makeText(MainActivity.this, "Start download", Toast.LENGTH_SHORT).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                 Log.w(TAG, throwable);
                Toast.makeText(MainActivity.this, "Failed to add task", Toast.LENGTH_SHORT).show();
            }
        });
//Need missionId, can be any string
//You can pause or delete all tasks for that bulk download by the missionId
//The missionId can be used to query the download of all the bulk downloads
```

3.Receive download events and download status.

```java
//The receiving event can be received anywhere, regardless of whether the task is started or not.
Disposable disposable =  mRxDownload.receiveDownloadStatus(url)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent event) throws Exception {
                        //When the event is Failed, there is an exception message, and the rest is null.
                        if (event.getFlag() == DownloadFlag.FAILED) { 
                            Throwable throwable = event.getError();
                            Log.w("Error", throwable);
                        }
                        mDownloadController.setEvent(event);
                        updateProgress(event);
                    }
                });
// Dispose when the activity is destroyed, dispose the Disposable to cancel the receiving event, but will not suspend the download.
// Regardless of whether the task starts to download, can get the corresponding events and status of the url.
// Will only receive onNext events, will not receive onError and onComplete events, so just listen on the onNext.
```

4.Pause download

```java
//Single pause, pause address for url download task
rxDownload.pauseServiceDownload(url).subscribe();

//Suspension, pause all tasks represented by missionId
rxDownload.pauseServiceDownload(missionId).subscribe();
```

5.Continue to download

```java
//Re-call the download () method, and pass the same url.
RxDownload.getInstance(this)
        .serviceDownload(url) 
        ...
```

6.Delete the download

```java
//Pause the address of the url download and delete records from the database, deleteFile is true will delete the url to download all the files
rxDownload.deleteServiceDownload(url, deleteFile).subscribe();

//Batch delete, suspend all tasks represented by missionId, and delete records for all tasks
rxDownload.deleteServiceDownload(missionId,deleteFile).subscribe();
```

7.Form of transferform

```java
//single url
.compose(rxDownload.<Object>transformService(url))

//multi url
.compose(rxDownload.<Object>transformMulti(missionId,url1,url2,url3)) 
```

### Get download records

Get all the download records in the database

```java
mRxDownload.getTotalDownloadRecords()
    .subscribe(new Action1<List<DownloadRecord>>() {
        @Override
        public void call(List<DownloadRecord> list) {
            mAdapter.addAll(list);
        }
    });
```

### Get the download file

Get the download file

```java
//Use url to get
File[] files = rxDownload.getRealFiles(url);
if (files != null) {
	File file = files[0];
}
//Use saveName and savePath to get
File file = rxDownload.getRealFiles(saveName,savePath)[0];
```

### About me

If you want to communicate with the project, you can do the following:

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